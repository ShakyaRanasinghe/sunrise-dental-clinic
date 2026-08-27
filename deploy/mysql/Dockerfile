# Sunrise Dental Clinic — database image.
#
# The official MySQL image runs every *.sql in /docker-entrypoint-initdb.d/ on
# first boot only (when /var/lib/mysql is empty). The numeric prefixes keep the
# files in dependency order: schema before procedures and demo data. Render
# mounts a persistent disk at /var/lib/mysql (see render.yaml).
FROM mysql:8.0
COPY modular/src/main/resources/schema.sql     /docker-entrypoint-initdb.d/01-schema.sql
COPY modular/src/main/resources/procedures.sql /docker-entrypoint-initdb.d/02-procedures.sql
COPY modular/src/main/resources/demo-data.sql  /docker-entrypoint-initdb.d/03-demo-data.sql
EXPOSE 3306