CREATE USER "odk" WITH UNENCRYPTED PASSWORD 'odk';
CREATE SCHEMA "odk_sync" AUTHORIZATION "odk";
GRANT ALL PRIVILEGES ON SCHEMA "odk_sync" TO "odk";
      