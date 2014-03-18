CREATE TABLE `ships_modules` (
	id integer not null auto_increment,
	`adocks` integer not null,
	`ablativeArmor` integer not null,
	`bounty` decimal(19,2) not null,
	`cargo` bigint not null,
	`cost` integer not null,
	`crew` integer not null,
	`deutfactor` integer not null,
	`eps` integer not null,
	`flags` longtext not null,
	`heat` integer not null,
	`hull` integer not null,
	`hydro` integer not null,
	`jdocks` integer not null,
	`lostInEmpChance` double precision not null,
	`maxheat` longtext not null,
	`maxunitsize` integer not null,
	`minCrew` integer not null,
	`modules` longtext not null,
	`nahrungcargo` bigint not null,
	`nickname` varchar(255) not null,
	`panzerung` integer not null,
	`pickingCost` integer not null,
	`picture` varchar(255) not null,
	`ra` integer not null,
	`rd` integer not null,
	`recost` integer not null,
	`rm` integer not null,
	`ru` integer not null,
	`scanCost` integer not null,
	`sensorrange` integer not null,
	`shields` integer not null,
	`size` integer not null,
	`srs` boolean not null,
	`torpedodef` integer not null,
	`unitspace` integer not null,
	`version` integer not null,
	`versorger` boolean not null,
	`weapons` longtext not null,
  `werft` integer not null,
	`ow_werft` integer,
	primary key (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;