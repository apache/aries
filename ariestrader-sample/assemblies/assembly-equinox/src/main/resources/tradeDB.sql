connect 'jdbc:derby:tradeDB;create=true';
create table holdingejb
  (purchaseprice decimal(10, 2),
   holdingid integer not null,
   quantity double not null,
   purchasedate timestamp,
   account_accountid integer,
   quote_symbol varchar(250),
   optLock integer);

alter table holdingejb
  add constraint pk_holdingejb primary key (holdingid);

create table accountprofileejb
  (address varchar(250),
   passwd varchar(250),
   userid varchar(250) not null,
   email varchar(250),
   creditcard varchar(250),
   fullname varchar(250),
   optLock integer);

alter table accountprofileejb
  add constraint pk_accountprofile2 primary key (userid);

create table quoteejb
  (low decimal(10, 2),
   open1 decimal(10, 2),
   volume double not null,
   price decimal(10, 2),
   high decimal(10, 2),
   companyname varchar(250),
   symbol varchar(250) not null,
   change1 double not null,
   optLock integer);

alter table quoteejb
  add constraint pk_quoteejb primary key (symbol);

create table keygenejb
  (keyval integer not null,
   keyname varchar(250) not null);

alter table keygenejb
  add constraint pk_keygenejb primary key (keyname);

create table accountejb
  (creationdate timestamp,
   openbalance decimal(10, 2),
   logoutcount integer not null,
   balance decimal(10, 2),
   accountid integer not null,
   lastlogin timestamp,
   logincount integer not null,
   PROFILE_USERID VARCHAR(250),
   optLock integer);

alter table accountejb
  add constraint pk_accountejb primary key (accountid);

create table orderejb
  (orderfee decimal(10, 2),
   completiondate timestamp,
   ordertype varchar(250),
   orderstatus varchar(250),
   price decimal(10, 2),
   quantity double not null,
   opendate timestamp,
   orderid integer not null,
   account_accountid integer,
   quote_symbol varchar(250),
   holding_holdingid integer,
   optLock integer);

alter table orderejb
  add constraint pk_orderejb primary key (orderid);

create index profile_userid on accountejb(profile_userid);
create index account_accountid on holdingejb(account_accountid);
create index account_accountidt on orderejb(account_accountid);
create index holding_holdingid on orderejb(holding_holdingid);
create index orderstatus on orderejb(orderstatus);
create index ordertype on orderejb(ordertype);
exit;
