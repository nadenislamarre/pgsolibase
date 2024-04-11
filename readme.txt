
=== db files ===
data/db1.sql
data/db2.sql

=== Execute ===
sudo docker run -p 5432:5432 -e POSTGRES_PASSWORD=toto postgres
CLASSPATH=postgresql-42.7.3.jar:$PWD java PGSolibase jdbc:postgresql://localhost:5432/postgres postgres toto public data
