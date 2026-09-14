# Архитектура

```text
controller → service → repository → PostgreSQL
                 ↓
          Agent и Action clients
```

- Controller переводит generated HTTP model в простой service command.
- Service управляет одним сообщением и транзакцией.
- Repository использует только jOOQ и свою базу.
- Agent client получает предложение, Action client создаёт действие.

Каркас пока не содержит endpoint, clients и таблиц. Это следующий пакет после решения о приватности
исходного текста сообщения.
