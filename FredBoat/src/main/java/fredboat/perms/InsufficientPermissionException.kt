package fredboat.perms

class InsufficientPermissionException(
        val permissions: IPermissionSet,
        override val message: String = "Missing permissions: $permissions"
) : RuntimeException()