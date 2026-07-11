## Slide Objective
讲清楚三种存储的适用场景。

## Suggested Presentation
Redis、MySQL、Vector DB——三种存储解决三种问题。

Redis 存临时的。Session Memory 用 Redis，因为对话历史需要快速读写、不需要永久保存。Redis 的 List 结构天然适合消息队列。验证：redis-cli LRANGE session:xxx:messages 0 -1。

MySQL 存持久的。用户画像用 MySQL，因为画像需要跨 Session 永久生效、需要结构化查询。建表脚本在 docs/schema.sql。

Vector DB 存知识的。企业文档用向量数据库，下节课引入。语义检索——用户问"笔记本卡顿怎么办"，匹配到"电脑运行缓慢的解决方案"。

选型很简单：临时数据用 Redis，持久数据用 MySQL，语义检索用 Vector DB。不要用错。

## Transition
存储选型讲完了。最后看生产环境注意事项。
