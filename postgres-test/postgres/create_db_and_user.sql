create database "odk_sync";
        create user "odk" with password 'odk';
        grant all privileges on database "odk_sync" to "odk";
        alter database "odk_sync" owner to "odk";
        \c "odk_sync";
        create schema "odk_sync";
        grant all privileges on schema "odk_sync" to "odk";
        alter schema "odk_sync" owner to "odk";
      