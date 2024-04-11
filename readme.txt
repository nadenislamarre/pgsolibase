=== Replace liquibase by a simple class ===
- keep things simple for a simple need
- use true locks (versus manual locks to remove in database in case of application crash)
- use simple versionning : just a number. not a list of hashes. ease of comparing environement.
- no need to maintain yet an other library
- make it possible to modify ddl sql files: it is really a real case need. sometimes, after upgrades (sql options no more valid), renaming, mistakes, it is required.
- don't think that reverting is just a ddl story
- use specific postgresql commands (and don't limit your code possibility just so that it works on any database server)

=== Usage ===
PGSolibase searches in database your current version (0 if nothing is found).
Then plays sql files which are in the data folder starting from db1.sql, db2.sql...
When you rerun, it searches the current version (for example 53).
Then, if found, it will plays db54.sql, and so on.

Just create a folder with ddl files.
One by one.
db1.sql is delivered with the tool and contains the dbparameters table creation.

Put files in the data directory :
data/db1.sql
data/db2.sql
data/db3.sql

And just execute :
java PGSolibase jdbc:postgresql://localhost:5432/postgres login password dbparameter_table_schema ddl_directory
(or include in your project and call PGSolibase.upgradeDB())

In case of error, the ddl is rolledback and your version number remains unchanged (back to the last valid version).

=== Example
$ java PGSolibase
current db version : 52
upgrading schema to version 53
sql:
create table orders(id serial primary key, name varchar(32));
-----
commit
upgrading schema to version 54
sql:
create table people(id serial primary key, firstname varchar(32), lastname varchar(32));
create table colors(id serial primary key, name varchar(32));
-----
commit
$ java PGSolibase
current db version : 54
$

=== Memo test via docker ===
sudo docker run -p 5432:5432 -e POSTGRES_PASSWORD=toto postgres
CLASSPATH=postgresql-42.7.3.jar:$PWD java PGSolibase jdbc:postgresql://localhost:5432/postgres postgres toto public data
