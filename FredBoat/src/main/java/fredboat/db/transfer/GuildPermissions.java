/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.db.transfer;

import fredboat.definitions.PermissionLevel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by napster on 20.03.18.
 * <p>
 * Transfer object for the {@link fredboat.db.entity.main.GuildPermissions}
 */
//todo move ""business"" logic to the backend
public class GuildPermissions implements TransferObject<String> {

    // Guild ID
    private String id;
    private String adminList = "";
    private String djList = "";
    private String userList = "";

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public List<String> getAdminList() {
        return Arrays.asList(adminList.split(" "));
    }

    public GuildPermissions setAdminList(List<String> list) {
        StringBuilder str = new StringBuilder();
        for (String item : list) {
            str.append(item).append(" ");
        }

        adminList = str.toString().trim();
        return this;
    }

    public List<String> getDjList() {
        return Arrays.asList(djList.split(" "));
    }

    public GuildPermissions setDjList(List<String> list) {
        StringBuilder str = new StringBuilder();
        for (String item : list) {
            str.append(item).append(" ");
        }

        djList = str.toString().trim();
        return this;
    }

    public List<String> getUserList() {
        return Arrays.asList(userList.split(" "));
    }

    public GuildPermissions setUserList(List<String> list) {
        StringBuilder str = new StringBuilder();
        for (String item : list) {
            str.append(item).append(" ");
        }

        userList = str.toString().trim();
        return this;
    }

    public List<String> getFromEnum(PermissionLevel level) {
        switch (level) {
            case ADMIN:
                return getAdminList();
            case DJ:
                return getDjList();
            case USER:
                return getUserList();
            default:
                throw new IllegalArgumentException("Unexpected enum " + level);
        }
    }

    public GuildPermissions setFromEnum(PermissionLevel level, List<String> list) {
        switch (level) {
            case ADMIN:
                return setAdminList(list);
            case DJ:
                return setDjList(list);
            case USER:
                return setUserList(list);
            default:
                throw new IllegalArgumentException("Unexpected enum " + level);
        }
    }

}
