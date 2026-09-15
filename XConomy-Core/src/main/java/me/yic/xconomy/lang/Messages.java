/*
 *  This file (Messages.kt) is a part of project XConomy
 *  Copyright (C) YiC and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package me.yic.xconomy.lang;

public class Messages {
    public String gettag(String message) {
        if (message == null) return null;
        return switch (message) {
            case "数据保存方式" -> "saving-mode";
            case "自定义文件夹路径不存在" -> "no-custom-path";
            case "连接正常" -> "connect-success";
            case "连接异常" -> "connect-fail";
            case "重新连接成功" -> "reconnect-success";
            case "连接断开失败" -> "disconnect-fail";
            case "缓存文件创建异常" -> "cache-file-creation-exception";
            case "升级数据库表格。。。" -> "upgrade-database";
            case "Redis监听线程创建中" -> "redis-create";
            case "订阅Redis频道成功, channel " -> "redis-subscribe";
            case "取消订阅Redis频道" -> "redis-unsubscribe";
            case "XConomy加载成功" -> "enable-success";
            case "XConomy已成功卸载" -> "disable-success";
            case "已开启BungeeCord同步" -> "enable-bungeecord";
            case "SQLite文件路径设置错误" -> "custom-path-error";
            case "文件夹创建异常" -> "create-folder-fail";
            case "BungeeCord同步未开启" -> "not-enable-bungeecord";
            case "无法连接到数据库-----" -> "unable-connect";
            case "JDBC驱动加载失败" -> "jdbc-fail";
            case "已创建一个新的语言文件" -> "create-language-file-success";
            case "语言文件创建异常" -> "create-language-file-fail";
            case "发现 PlaceholderAPI" -> "found-placeholderapi";
            case "发现 DatabaseDrivers" -> "found-databasedrivers";
            case "已是最新版本" -> "is-new-version";
            case "检查更新失败" -> "check-version-fail";
            case "发现新版本 " -> "found-version";
            case "§amessage.yml重载成功" -> "messege-reload";
            case "vault-baltop-tips-a" -> "vault-baltop-tips-a";
            case "vault-baltop-tips-b" -> "vault-baltop-tips-b";
            case " 名称已更改!" -> "username-modified";
            case "§cBC模式开启的情况下,无法在无人的服务器中使用OP命令" -> "no-player-tips";
            case "§c该指令不支持在半正版模式中使用" -> "semi-mode-ban-commands";
            case "§6控制台无法使用该指令" -> "console-ban-commands";
            case "连接池未启用" -> "pool-disable";
            case "未找到 'org.slf4j.Logger'" -> "slf4j-unfound";
            case "收到不同版本插件的数据，无法同步，当前插件版本 " -> "different-version";
            default -> message;
        };
    }
}
