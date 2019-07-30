create table log_records
(
  LOGDATE         bigint       not null,
  IP              varchar(15)  not null,
  REQUEST         varchar(200) not null,
  RESPONSE_STATUS int(3)       not null,
  USER_AGENT      varchar(500) not null,
  LOG_ID          int auto_increment
    primary key
);

create table blocked_ips
(
  IP      varchar(15)  not null
    primary key,
  COMMENT varchar(200) null
);


