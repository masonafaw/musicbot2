package fredboat.sentinel

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.*
import fredboat.config.ApplicationInfo
import fredboat.perms.IPermissionSet
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class Sentinel(private val template: AsyncRabbitTemplate,
               private val blockingTemplate: RabbitTemplate,
               val tracker: SentinelTracker,
               val applicationInfo: ApplicationInfo,
               val selfUser: RawUser) {

    fun sendAndForget(routingKey: String, request: Any) {
        blockingTemplate.convertAndSend(SentinelExchanges.REQUESTS, routingKey, request)
    }

    fun <T> send(
            guild: Guild,
            request: Any,
            exchange: String = SentinelExchanges.REQUESTS
    ): Mono<T> = send(guild.routingKey, request, exchange)

    fun <T> send(
            routingKey: String,
            request: Any,
            exchange: String = SentinelExchanges.REQUESTS
    ): Mono<T> = Mono.create<T> {
        template.convertSendAndReceive<T>(exchange, routingKey, request)
                .addCallback(
                        { res ->
                            if (res != null) it.success(res) else it.success()
                        },
                        { exc -> it.error(exc) }
                )
    }

    fun <R, T> genericMonoSendAndReceive(
            exchange: String = SentinelExchanges.REQUESTS,
            routingKey: String,
            request: Any,
            mayBeEmpty: Boolean = false,
            deliveryMode: MessageDeliveryMode = MessageDeliveryMode.NON_PERSISTENT,
            transform: (response: R) -> T): Mono<T> = Mono.create<T> {
        val postProcessor = MessagePostProcessor { processor ->
            processor.messageProperties.deliveryMode = deliveryMode; processor
        }
        template.convertSendAndReceive<R?>(exchange, routingKey, request, postProcessor).addCallback(
                { res ->
                    try {
                        if (res == null) {
                            if (mayBeEmpty) it.success()
                            else it.error(SentinelException("RPC response was null"))
                        } else it.success(transform(res))
                    } catch (e: Exception) {
                        it.error(e.asCause(request))
                    }
                },
                { t ->
                    it.error(t.asCause(request))
                }
        )
    }

    fun sendMessage(routingKey: String, channel: TextChannel, message: String): Mono<SendMessageResponse> =
            genericMonoSendAndReceive<SendMessageResponse, SendMessageResponse>(
                    SentinelExchanges.REQUESTS,
                    routingKey,
                    SendMessageRequest(channel.id, message),
                    mayBeEmpty = false,
                    transform = { it }
            )

    fun sendMessage(routingKey: String, channel: TextChannel, message: Embed): Mono<SendMessageResponse> =
            genericMonoSendAndReceive<SendMessageResponse, SendMessageResponse>(
                    SentinelExchanges.REQUESTS,
                    routingKey,
                    SendEmbedRequest(channel.id, message),
                    mayBeEmpty = false,
                    transform = { it }
            )

    fun sendPrivateMessage(user: User, message: String): Mono<SendMessageResponse> =
            genericMonoSendAndReceive<SendMessageResponse, SendMessageResponse>(
                    SentinelExchanges.REQUESTS,
                    tracker.getKey(0),
                    SendPrivateMessageRequest(user.id, message),
                    mayBeEmpty = true,
                    transform = {it}
            )

    fun editMessage(channel: TextChannel, messageId: Long, message: String): Mono<Unit> =
            genericMonoSendAndReceive<Unit, Unit>(
                    SentinelExchanges.REQUESTS,
                    channel.guild.routingKey,
                    EditMessageRequest(channel.id, messageId, message),
                    mayBeEmpty = true,
                    transform = {}
            )

    fun deleteMessages(channel: TextChannel, messages: List<Long>): Mono<Unit> =
            genericMonoSendAndReceive<Unit, Unit>(
                    SentinelExchanges.REQUESTS,
                    channel.guild.routingKey,
                    MessageDeleteRequest(channel.id, messages),
                    mayBeEmpty = true,
                    transform = {}
            )

    fun sendTyping(channel: TextChannel) {
        val req = SendTypingRequest(channel.id)
        blockingTemplate.convertAndSend(SentinelExchanges.REQUESTS, channel.guild.routingKey, req)
    }

    /* Permissions */

    private fun checkPermissions(member: Member?, role: Role?, permissions: IPermissionSet): Mono<PermissionCheckResponse> {
        val guild = member?.guild ?: role!!.guild

        return genericMonoSendAndReceive<PermissionCheckResponse, PermissionCheckResponse>(
                SentinelExchanges.REQUESTS,
                guild.routingKey,
                GuildPermissionRequest(guild.id, role = role?.id, member = member?.id, rawPermissions = permissions.raw),
                mayBeEmpty = true,
                transform = { it }
        )
    }

    // Role and member are mutually exclusive
    fun checkPermissions(member: Member, permissions: IPermissionSet) = checkPermissions(member, null, permissions)

    fun checkPermissions(role: Role, permissions: IPermissionSet) = checkPermissions(null, role, permissions)

    fun checkPermissions(channel: Channel, member: Member?, role: Role?, permissions: IPermissionSet): Mono<PermissionCheckResponse> {
        val guild = member?.guild ?: role!!.guild

        return genericMonoSendAndReceive<PermissionCheckResponse, PermissionCheckResponse>(
                SentinelExchanges.REQUESTS,
                guild.routingKey,
                ChannelPermissionRequest(channel.id, member?.id, role?.id, permissions.raw),
                mayBeEmpty = true,
                transform = { it }
        )
    }

    // Role and member are mutually exclusive
    fun checkPermissions(channel: Channel, member: Member, permissions: IPermissionSet) = checkPermissions(channel, member, null, permissions)

    fun checkPermissions(channel: Channel, role: Role, permissions: IPermissionSet) = checkPermissions(channel, null, role, permissions)

    /**
     * Takes [members] and maps them to their effective permissions.
     *
     * @throws [IllegalArgumentException] if any member is not of the [guild]
     */
    fun getPermissions(guild: Guild, members: List<Member>): Flux<Long> {
        val req = BulkGuildPermissionRequest(guild.id, members.map {
            if (it.guild.id != guild.id) throw IllegalArgumentException("All members must be of the same guild")
            it.id
        })

        return Flux.create { sink ->
            template.convertSendAndReceive<BulkGuildPermissionResponse>(SentinelExchanges.REQUESTS, guild.routingKey, req).addCallback(
                    { r ->
                        r!!.effectivePermissions.forEach { sink.next(it ?: 0) }
                        sink.complete()
                    },
                    { exc -> sink.error(exc.asCause(req)) }
            )
        }
    }

    fun getBanList(guild: Guild): Flux<Ban> {
        val req = BanListRequest(guild.id)
        return Flux.create { sink ->
            template.convertSendAndReceive<Array<Ban>>(SentinelExchanges.REQUESTS, guild.routingKey, req).addCallback(
                    { r ->
                        r!!.forEach { sink.next(it) }
                        sink.complete()
                    },
                    { exc -> sink.error(exc.asCause(req)) }
            )
        }
    }

    /* Extended info requests */

    fun getGuildInfo(guild: Guild): Mono<GuildInfo> =
            genericMonoSendAndReceive<GuildInfo, GuildInfo>(
                    SentinelExchanges.REQUESTS,
                    guild.routingKey,
                    GuildInfoRequest(guild.id),
                    mayBeEmpty = false,
                    transform = { it }
            )

    fun getMemberInfo(member: Member): Mono<MemberInfo> =
            genericMonoSendAndReceive<MemberInfo, MemberInfo>(
                    SentinelExchanges.REQUESTS,
                    member.guild.routingKey,
                    MemberInfoRequest(member.id, member.guild.id),
                    mayBeEmpty = false,
                    transform = { it }
            )

    fun getRoleInfo(role: Role): Mono<RoleInfo> =
            genericMonoSendAndReceive<RoleInfo, RoleInfo>(
                    SentinelExchanges.REQUESTS,
                    role.guild.routingKey,
                    RoleInfoRequest(role.id),
                    mayBeEmpty = false,
                    transform = { it }
            )

    fun getUser(id: Long, routingKey: String): Mono<User> = genericMonoSendAndReceive<User, User>(
            exchange = SentinelExchanges.REQUESTS,
            routingKey = routingKey,
            mayBeEmpty = true,
            request = GetUserRequest(id),
            transform = { it }
    )

    /* Mass requests */

    data class NamedSentinelInfoResponse(
            val response: SentinelInfoResponse,
            val routingKey: String
    )

    private fun getSentinelInfo(routingKey: String, includeShards: Boolean = false) =
            genericMonoSendAndReceive<SentinelInfoResponse, NamedSentinelInfoResponse>(
                    SentinelExchanges.REQUESTS,
                    routingKey,
                    SentinelInfoRequest(includeShards),
                    mayBeEmpty = true,
                    transform = { NamedSentinelInfoResponse(it, routingKey) }
            )

    /** Request sentinel info from each tracked Sentinel simultaneously in the order of receiving them
     *  Errors are delayed till all requests have either completed or failed */
    fun getAllSentinelInfo(includeShards: Boolean = false): Flux<NamedSentinelInfoResponse> {
        return Flux.mergeSequentialDelayError(
                tracker.sentinels.map { getSentinelInfo(it.key, includeShards) },
                2,
                1 // Not sure what this is -- hardly even documented
        )
    }

    private fun getSentinelUserList(routingKey: String): Flux<Long> = Flux.create { sink ->
        template.convertSendAndReceive<List<Long>>(SentinelExchanges.REQUESTS, routingKey, UserListRequest()).addCallback(
                { list ->
                    if (list == null) sink.error(NullPointerException())
                    list!!.forEach { sink.next(it) }
                    sink.complete()
                },
                { e -> sink.error(e.asCause("UserListRequest")) }
        )
    }

    /** Request user IDs from each tracked Sentinel simultaneously in the order of receiving them
     *  Errors are delayed till all requests have either completed or failed */
    fun getFullSentinelUserList(): Flux<Long> {
        return Flux.mergeSequentialDelayError(
                tracker.sentinels.map { getSentinelUserList(it.key) },
                1,
                1 // Not sure what this is -- hardly even documented
        )
    }

    /** Provides a trace of the actual method invoked */
    private fun Throwable.asCause(request: Any) = SentinelException("Sentinel request failed $request", this)

}