# ReportSystem

Minecraft 服务端举报插件，覆盖 Bukkit 系与代理端。举报数据落在共享数据库里，各端独立可用，
也可以把代理端和后端指向同一个库，实现跨服查看与跳转。

## 版本与产物

七个平台由五份 jar 覆盖，按服务端版本选一个装上即可。

| jar | 覆盖范围 | 编译目标 |
| --- | --- | --- |
| `ReportSystem-Bukkit-1.8.8.jar` | Bukkit / Spigot / Paper 1.8.8 ~ 1.20.4 | Java 8 |
| `ReportSystem-Paper-1.21.11.jar` | Paper 1.20.5 ~ 1.21.11，含 Folia | Java 21 |
| `ReportSystem-Paper-26.3.jar` | Paper 26.x，含 Folia | Java 25 |
| `ReportSystem-Velocity.jar` | Velocity 3.x | Java 21 |
| `ReportSystem-Bungee.jar` | BungeeCord | Java 17+ |

Folia 不需要单独的 jar，Paper 产物已经声明了 `folia-supported`，启动时自动识别并切到区域化调度器。

WaterFall 是 BungeeCord 的 fork，`ReportSystem-Bungee.jar` 可以直接放进去跑。但 WaterFall 已于
2024 年 3 月停止维护，这里只做兼容，不对后续版本负责。

## 安装

1. 把对应版本的 jar 放进 `plugins/`（代理端就是代理的 `plugins/`）。
2. 启动一次，生成 `config.yml`、`categories.yml`、`actions.yml` 和 `lang/` 语言目录。
3. 改 `config.yml` 里的 `server-id` 和 `storage`。
4. 重启或执行 `/reports reload`。

### server-id 怎么填

`server-id` 是这台服务端在网络里的标识。单服随便填，比如 `survival`。

多服必须和代理配置里的服务器名一致。Velocity 是 `velocity.toml` 的 `[servers]` 键名，BungeeCord 是
`config.yml` 的 `servers` 键名。写错了不会报错，但跨服跳转会认不出目标子服。

代理端的 `server-id` 只作为兜底，实际记录的是玩家当时所在的子服。

## 数据库

四种都支持，`storage.type` 可选 `sqlite`、`mysql`、`mariadb`、`postgresql`。表结构在首次启动时自动创建。

SQLite 开箱即用，适合单服：

```yaml
storage:
  type: sqlite
  file: reports.db
```

多服用远程库，建库语句：

```sql
CREATE DATABASE reportsystem DEFAULT CHARACTER SET utf8mb4;
```

PostgreSQL 用 `CREATE DATABASE reportsystem;` 即可，字符集跟着模板库走。

多个服务端指向同一个库，就能互相看到举报。代理端和后端同时装的时候，后端会在举报里写入坐标、世界和
聊天快照，代理端负责跨服跳转。

## 命令

| 命令 | 说明 |
| --- | --- |
| `/report <玩家> <理由>` | 提交举报 |
| `/report gui` | 打开举报菜单（仅后端） |
| `/reports list [状态] [页码]` | 查看举报列表，状态可填 `pending`、`claimed`、`resolved`、`rejected`、`all` |
| `/reports view <编号>` | 查看详情与处理记录 |
| `/reports claim <编号>` | 认领 |
| `/reports resolve <编号> [备注]` | 标记已处理 |
| `/reports reject <编号> [备注]` | 驳回 |
| `/reports tp <编号>` | 传送到举报现场 |
| `/reports stats` | 各状态数量 |
| `/reports reload` | 重载配置 |
| `/language [code\|list]` | 查看或切换自己的语言 |

代理端没有物品栏，`/report gui` 会提示改用命令。

## 权限

| 节点 | 默认 | 说明 |
| --- | --- | --- |
| `reportsystem.use` | 所有人 | 提交举报 |
| `reportsystem.language` | 所有人 | 切换自己的语言 |
| `reportsystem.admin` | OP | 查看与处理举报 |
| `reportsystem.admin.teleport` | OP | 跳转现场 |
| `reportsystem.admin.reload` | OP | 重载配置 |
| `reportsystem.notify` | OP | 上线时提示待处理数量 |
| `reportsystem.bypass.cooldown` | 无 | 跳过冷却与每日上限 |

## 配置说明

### config.yml

```yaml
server-id: survival          # 多服时与代理里的服务器名一致
default-category: general    # 命令举报时使用的分类
language: zh_CN              # 服务器默认语言，对应 lang/ 下的文件名

limits:
  cooldown-seconds: 60       # 两次提交之间的间隔
  daily-limit: 10            # 每人每天最多提交数
  duplicate-window-minutes: 30  # 同一对举报人与被举报人在此时间内不允许重复
  max-reason-length: 200
```

冷却、每日上限、重复提交的判断都走数据库，多服之间是一致的。

### categories.yml

举报类型。`material` 用 Bukkit 的材质名，`slot` 是菜单里的位置，`aliases` 可以直接在命令里当分类名用。

默认材质都选了 1.8 就存在的物品，换材质时注意老版本可能没有。

### actions.yml

结案时执行的命令，以控制台身份执行，支持 `%id%`、`%target%`、`%reporter%`、`%handler%`、`%reason%` 占位符。

插件本身不封禁任何人，要接处罚就写在这里：

```yaml
on-resolve:
  - "ban %target% 被举报违规，编号 %id%"
```

点击菜单里的分类后，玩家需要在聊天栏输入理由；`gui-cancel-keyword` 是放弃输入的关键词。

## 多语言

语言文件放在插件目录的 `lang/` 下，一个语言一个文件：

```
plugins/ReportSystem/lang/
├── index.txt      ← 语言清单，每行一个 locale，新增语言要在这里登记
├── zh_CN.yml
└── en_US.yml
```

`config.yml` 的 `language` 决定服务器默认语言；玩家可以用 `/language <代码>` 切换自己的语言，
选择会存进数据库的 `rs_player_locale` 表，所以在装了共享库的网络里跨子服一致。
`/language list` 列出当前可用语言。

新增语言：复制一份 `zh_CN.yml` 改成目标语言，文件名即语言代码（如 `ja_JP.yml`），
然后在 `index.txt` 里加一行 `ja_JP`，`/reports reload` 即可生效。

某条文案在某个语言里缺失时会自动回落到默认语言，不会显示成 key 名。占位符（`%id%`、`%player%` 等）
必须与原文保持一致，改了名字玩家会看到字面量。

控制台与日志固定使用默认语言，不受 `/language` 影响。

## 关于跳转现场

`/reports tp` 的行为取决于部署方式：

- 管理员和举报现场在同一个子服：代理端改不了坐标，会把任务写进 `rs_pending_teleport` 并通过插件消息
  通知后端，由后端在自己线程里执行传送。
- 不在同一个子服：代理端先写任务再发起跳服，管理员进服后由后端的 `PlayerJoinEvent` 消费并传送。
- 只装后端：直接本服传送，目标子服名对不上时提示子服不存在。

任务有 5 分钟有效期，过期的会被丢弃。

## 构建

需要 JDK 21 编译大部分模块，26.3 产物额外需要 JDK 25。

```bash
mvn clean package
```

`report-bukkit-modern` 默认产出 1.21.11 版本。要出 26.3 版本：

```bash
mvn -t toolchains.xml -pl report-bukkit-modern -am -Pmc26 package
```

`toolchains.xml` 里写的是本机 JDK 路径，换机器要改。不指定 `-t` 的话 Maven 会去找
`~/.m2/toolchains.xml`。

## 已知限制

- 代理端只能看到在线玩家，`/report <名字>` 对已下线且当前不在线的玩家会提示找不到。后端没有这个限制。
- 1.8.8 没有公开的客户端版本接口，legacy 产物沿 CraftPlayer 的 nms 链路取协议号，取不到就留空。
- Paper 只提供客户端品牌（`getClientBrandName`），拿不到协议版本，证据里记录的客户端标识是品牌名。
- 举报证据里的聊天记录来自内存环形缓存，重启后丢失，只保留重启之后的内容。
