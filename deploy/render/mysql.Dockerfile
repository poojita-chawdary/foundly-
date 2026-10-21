FROM mysql:8.4
# MySQL creates the empty database and account from MYSQL_* on first boot.
# The application runs the canonical Flyway migration; do not import it here.
CMD ["mysqld", "--require-secure-transport=ON", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci"]
