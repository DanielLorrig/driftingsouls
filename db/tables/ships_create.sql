CREATE TABLE `ships` (
  `id` integer not null,
	`ablativeArmor` integer not null,
	`alarm` integer not null,
	`battleAction` boolean not null,
	`cargo` longtext not null,
	`comm` integer not null,
	`crew` integer not null,
	`docked` varchar(255) not null,
	`e` integer not null,
	`engine` integer not null,
	`s` integer not null,
	`hull` integer not null,
	`jumptarget` varchar(255) not null,
	`nahrungcargo` bigint not null,
	`name` varchar(255) not null,
	`oncommunicate` longtext,
	`sensors` integer not null,
	`shields` integer not null,
	`status` varchar(255) not null,
	`system` integer not null,
	`version` integer not null,
	`heat` longtext not null,
	`weapons` integer not null,
	`x` integer not null,
	`y` integer not null,
	`battle` integer,
	einstellungen_id integer,
	`fleet` integer,
	`modules` integer,
	`owner` integer not null,
	`scriptData_id` integer,
	`type` integer not null,
  primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;