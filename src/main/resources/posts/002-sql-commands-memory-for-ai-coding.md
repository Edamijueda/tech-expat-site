---
slug: sql-commands-memory-for-ai-coding
order: 2
title: Memory for SQL Command in AI Age
author: Tobi Omorubore
date: 2026-09-18
description: Keeping a running log of SQL commands so an AI coding assistant has the context it needs without me re-deriving past decisions.
ai_percent: 0
sections:
  - blog
---

Working on a database for Industrial Equipment, I am aware of a need to remember the SQL commands I had run on the database, so that I could copy & add the SQL command to my prompt as I instruct an AI coding assistant to help get a task done. Without a memory, I would have to write queries all the time to understand decisions I have made on the database.

## What I did

- Created an `industrial_equipment_sql_command.txt` file as my memory.
- Added SQL commands of successful queries that manipulate the database.
- Copied & added the SQL cmd (or referenced the file) in my prompt.

## Example of content in the file

```sql
-- CREATE EQUIPMENT TABLE
create table equipment (
  id text primary key default gen_random_uuid()::text,
  name text not null,
  brand_id text references brands(id),
  specs jsonb,
  image_url text
);

-- OTHER SQL QUERIES
```

## Cons

Such a file could easily get populated & become difficult to review manually. When that time comes, I would extract commands into a separate unique file or come up with a better approach. But for now, the file is small, it serves my need, so I am keeping it.
