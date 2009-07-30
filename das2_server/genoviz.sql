-- MySQL Administrator dump 1.4
--
-- ------------------------------------------------------
-- Server version	5.0.67-community-nt


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


--
-- Create schema genoviz
--

CREATE DATABASE IF NOT EXISTS genoviz;
USE genoviz;

--
-- Definition of table `AnalysisType`
--

DROP TABLE IF EXISTS `AnalysisType`;
CREATE TABLE `AnalysisType` (
  `idAnalysisType` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(100) NOT NULL,
  `isActive` char(1) default NULL,
  `idUser` int(10) unsigned default NULL,
  PRIMARY KEY  USING BTREE (`idAnalysisType`),
  KEY `FK_AnalysisType_User` (`idUser`),
  CONSTRAINT `FK_AnalysisType_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `AnalysisType`
--

/*!40000 ALTER TABLE `AnalysisType` DISABLE KEYS */;
INSERT INTO `AnalysisType` (`idAnalysisType`,`name`,`isActive`,`idUser`) VALUES
 (1,'Dynamic/Differential','Y',NULL),
 (2,'Static','Y',NULL);
/*!40000 ALTER TABLE `AnalysisType` ENABLE KEYS */;


--
-- Definition of table `Annotation`
--

DROP TABLE IF EXISTS `Annotation`;
CREATE TABLE `Annotation` (
  `idAnnotation` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(45) NOT NULL,
  `description` varchar(2000) default NULL,
  `fileName` varchar(2000) default NULL,
  `idExperimentPlatform` int(10) unsigned default NULL,
  `idExperimentMethod` int(10) unsigned default NULL,
  `idAnalysisType` int(10) unsigned default NULL,
  `idGenomeVersion` int(10) unsigned NOT NULL,
  `codeVisibility` varchar(10) NOT NULL,
  `idUser` int(10) unsigned default NULL,
  `idSecurityGroup` int(10) unsigned default NULL,
  `summary` varchar(200) default NULL,
  PRIMARY KEY  (`idAnnotation`),
  KEY `FK_Annotation_ExperimentPlatform` (`idExperimentPlatform`),
  KEY `FK_Annotation_ExperimentMethod` (`idExperimentMethod`),
  KEY `FK_Annotation_GenomeVersion` (`idGenomeVersion`),
  KEY `FK_Annotation_User` (`idUser`),
  KEY `FK_Annotation_Visibility` (`codeVisibility`),
  KEY `FK_Annotation_group` USING BTREE (`idSecurityGroup`),
  KEY `FK_Annotation_QuantificationType` USING BTREE (`idAnalysisType`),
  CONSTRAINT `FK_Annotation_AnalysisType` FOREIGN KEY (`idAnalysisType`) REFERENCES `AnalysisType` (`idAnalysisType`),
  CONSTRAINT `FK_Annotation_ExperimentMethod` FOREIGN KEY (`idExperimentMethod`) REFERENCES `ExperimentMethod` (`idExperimentMethod`),
  CONSTRAINT `FK_Annotation_ExperimentPlatform` FOREIGN KEY (`idExperimentPlatform`) REFERENCES `ExperimentPlatform` (`idExperimentPlatform`),
  CONSTRAINT `FK_Annotation_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_Annotation_SecurityGroup` FOREIGN KEY (`idSecurityGroup`) REFERENCES `SecurityGroup` (`idSecurityGroup`),
  CONSTRAINT `FK_Annotation_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_Annotation_Visibility` FOREIGN KEY (`codeVisibility`) REFERENCES `Visibility` (`codeVisibility`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;


--
-- Definition of table `AnnotationGrouping`
--

DROP TABLE IF EXISTS `AnnotationGrouping`;
CREATE TABLE `AnnotationGrouping` (
  `idAnnotationGrouping` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(45) NOT NULL,
  `description` varchar(2000) default NULL,
  `idParentAnnotationGrouping` int(10) unsigned default NULL,
  `idGenomeVersion` int(10) unsigned default NULL,
  `idUser` int(10) unsigned default NULL,
  PRIMARY KEY  USING BTREE (`idAnnotationGrouping`),
  KEY `FK_AnnotationFolder_GenomeVersion` (`idGenomeVersion`),
  KEY `FK_AnnotationGrouping_parentAnnotationGrouping` USING BTREE (`idParentAnnotationGrouping`),
  KEY `FK_AnnotationGrouping_User` (`idUser`),
  CONSTRAINT `FK_AnnotationFolder_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_AnnotationGrouping_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_Annotationgruping_parentAnnotationGrouping` FOREIGN KEY (`idParentAnnotationGrouping`) REFERENCES `AnnotationGrouping` (`idAnnotationGrouping`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;
 
INSERT INTO `annotationgrouping` (`idAnnotationGrouping`,`name`,`description`,`idParentAnnotationGrouping`,`idGenomeVersion`,`idUser`) VALUES 
 (1,'C_elegans_Jan_2007','',NULL,6,NULL),
 (2,'C_elegans_May_2008','',NULL,11,NULL),
 (3,'D_rerio_Jul_2007','',NULL,5,NULL),
 (4,'H_sapiens_Mar_2006','',NULL,1,NULL),
 (5,'H_sapiens_May_2004','',NULL,9,NULL),
 (6,'M_abscessus_Mar_2008','',NULL,10,NULL),
 (7,'S_glossinidius_Jan_2006','',NULL,7,NULL),
 (8,'S_pombe_Apr_2007','',NULL,4,NULL);


--
-- Definition of table `AnnotationToAnnotationGrouping`
--

DROP TABLE IF EXISTS `AnnotationToAnnotationGrouping`;
CREATE TABLE `AnnotationToAnnotationGrouping` (
  `idAnnotation` int(10) unsigned NOT NULL,
  `idAnnotationGrouping` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idAnnotation`,`idAnnotationGrouping`),
  KEY `FK_AnnotationToAnnotationGrouping_AnnotationGrouping` (`idAnnotationGrouping`),
  CONSTRAINT `FK_AnnotationToAnnotationGrouping_AnnotationGrouping` FOREIGN KEY (`idAnnotationGrouping`) REFERENCES `AnnotationGrouping` (`idAnnotationGrouping`),
  CONSTRAINT `FK_AnnotationToGrouping_Annotation` FOREIGN KEY (`idAnnotation`) REFERENCES `Annotation` (`idAnnotation`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


--
-- Definition of table `ExperimentMethod`
--

DROP TABLE IF EXISTS `ExperimentMethod`;
CREATE TABLE `ExperimentMethod` (
  `idExperimentMethod` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(100) NOT NULL,
  `isActive` char(1) default NULL,
  `idUser` int(10) unsigned default NULL,
  PRIMARY KEY  (`idExperimentMethod`),
  KEY `FK_ExperimentMethod_User` (`idUser`),
  CONSTRAINT `FK_ExperimentMethod_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `ExperimentMethod`
--

/*!40000 ALTER TABLE `ExperimentMethod` DISABLE KEYS */;
INSERT INTO `ExperimentMethod` (`idExperimentMethod`,`name`,`isActive`,`idUser`) VALUES 
 (1,'chIP-seq','Y',NULL),
 (2,'chIP-chip','Y',NULL),
 (3,'CGN Microarray','Y',NULL),
 (4,'SNP Microarray','Y',NULL),
 (5,'Transcriptome Microarray','Y',NULL),
 (6,'Gene Expression Microarray','Y',NULL),
 (7,'Quantitative rtPCR','Y',NULL),
 (8,'SAGE','Y',NULL);
/*!40000 ALTER TABLE `ExperimentMethod` ENABLE KEYS */;


--
-- Definition of table `ExperimentPlatform`
--

DROP TABLE IF EXISTS `ExperimentPlatform`;
CREATE TABLE `ExperimentPlatform` (
  `idExperimentPlatform` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(100) NOT NULL,
  `isActive` char(1) default NULL,
  `idUser` int(10) unsigned default NULL,
  PRIMARY KEY  (`idExperimentPlatform`),
  KEY `FK_ExperimentPlatform_User` (`idUser`),
  CONSTRAINT `FK_ExperimentPlatform_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `ExperimentPlatform`
--

/*!40000 ALTER TABLE `ExperimentPlatform` DISABLE KEYS */;
INSERT INTO `ExperimentPlatform` (`idExperimentPlatform`,`name`,`isActive`,`idUser`) VALUES 
 (1,'Illumina Genome Analyzer','Y',NULL),
 (2,'Agilent Microarray','Y',NULL),
 (3,'Affymetrix Microarray','Y',NULL),
 (4,'454 Genome Sequencer','Y',NULL),
 (6,'Applied Biosystems SOLiD','Y',NULL),
 (7,'Helicos Genomic Signature Sequencing','Y',NULL);
/*!40000 ALTER TABLE `ExperimentPlatform` ENABLE KEYS */;


--
-- Definition of table `GenomeVersion`
--

DROP TABLE IF EXISTS `GenomeVersion`;
CREATE TABLE `GenomeVersion` (
  `idGenomeVersion` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(200) NOT NULL,
  `idOrganism` int(10) unsigned default NULL,
  `buildDate` datetime default NULL,
  `coordURI` varchar(2000) default NULL,
  `coordVersion` varchar(50) default NULL,
  `coordSource` varchar(50) default NULL,
  `coordTestRange` varchar(100) default NULL,
  `coordAuthority` varchar(50) default NULL,
  PRIMARY KEY  (`idGenomeVersion`),
  UNIQUE KEY `Index_GenomeVersionName` (`name`),
  KEY `FK_GenomeVersion_Organism` (`idOrganism`),
  CONSTRAINT `FK_GenomeVersion_Organism` FOREIGN KEY (`idOrganism`) REFERENCES `Organism` (`idOrganism`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `GenomeVersion`
--

/*!40000 ALTER TABLE `GenomeVersion` DISABLE KEYS */;
INSERT INTO `GenomeVersion` (`idGenomeVersion`,`name`,`idOrganism`,`buildDate`,`coordURI`,`coordVersion`,`coordSource`,`coordTestRange`,`coordAuthority`) VALUES 
 (1,'H_sapiens_Mar_2006',1,'2006-03-01 00:00:00','http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B36.1/','36','Chromosome','','NCBI'),
 (4,'S_pombe_Apr_2007',6,'2007-04-01 00:00:00','http://www.sanger.ac.uk/Projects/S_pombe/Apr_2007','Apr_2007','Chromosome','asdfasdf','Sanger'),
 (5,'D_rerio_Jul_2007',4,'2007-07-01 00:00:00','http://zfin.org/genome/D_rerio/Zv7/','Zv7','Chromosome',NULL,'ZFISH_7'),
 (6,'C_elegans_Jan_2007',3,'2007-01-01 00:00:00','http://www.wormbase.org/genome/C_elegans/WS180/','180','Chromosome','','WS'),
 (7,'S_glossinidius_Jan_2006',5,'2006-01-01 00:00:00','ftp://ftp.ncbi.nih.gov/genomes/Bacteria/Sodalis_glossinidius_morsitans/Jan_2006','Jan_2006','Chromosome',NULL,'NCBI'),
 (9,'H_sapiens_May_2004',1,'2004-05-01 00:00:00','http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B35.1/','35','Chromosome',NULL,'NCBI'),
 (10,'M_abscessus_Mar_2008',8,'2008-03-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (11,'C_elegans_May_2008',3,'2008-05-01 00:00:00',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `GenomeVersion` ENABLE KEYS */;


--
-- Definition of table `GenomeVersionAlias`
--

DROP TABLE IF EXISTS `GenomeVersionAlias`;
CREATE TABLE `GenomeVersionAlias` (
  `idGenomeVersionAlias` int(10) unsigned NOT NULL auto_increment,
  `alias` varchar(100) NOT NULL,
  `idGenomeVersion` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idGenomeVersionAlias`),
  KEY `FK_GenomeVersionAlias_GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_GenomeVersionAlias_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `GenomeVersionAlias`
--

/*!40000 ALTER TABLE `GenomeVersionAlias` DISABLE KEYS */;
INSERT INTO `GenomeVersionAlias` (`idGenomeVersionAlias`,`alias`,`idGenomeVersion`) VALUES 
 (1,'spApr07',6),
 (4,'sgJan06',7),
 (5,'danRer5',5),
 (6,'zv7',5),
 (7,'Zv7',5),
 (8,'hg17',9),
 (9,'hg18',1);
/*!40000 ALTER TABLE `GenomeVersionAlias` ENABLE KEYS */;


--
-- Definition of table `Organism`
--

DROP TABLE IF EXISTS `Organism`;
CREATE TABLE `Organism` (
  `idOrganism` int(10) unsigned NOT NULL auto_increment,
  `commonName` varchar(100) NOT NULL,
  `sortOrder` int(10) unsigned default NULL,
  `binomialName` varchar(200) NOT NULL,
  `NCBITaxID` varchar(45) default NULL,
  `idUser` int(10) unsigned default NULL,
  `name` varchar(200) NOT NULL,
  PRIMARY KEY  USING BTREE (`idOrganism`),
  UNIQUE KEY `Index_OrganismName` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Organism`
--

/*!40000 ALTER TABLE `Organism` DISABLE KEYS */;
INSERT INTO `Organism` (`idOrganism`,`commonName`,`sortOrder`,`binomialName`,`NCBITaxID`,`idUser`,`name`) VALUES 
 (1,'Human',1,'Homo sapiens','9606',NULL,'H_sapiens'),
 (2,'Mouse',6,'Mus musculus','10090',NULL,'M_musculus'),
 (3,'Nematode',2,'Caenorhabditis elegans','6239',NULL,'C_elegans'),
 (4,'Zebrafish',3,'Danio rerio','7955',NULL,'D_rerio'),
 (5,'S. glossinidius',7,'Sodalis glossinidius','343509',NULL,'S_glossinidius'),
 (6,'Fission yeast',4,'Schizosaccharomyces pombe','4896',NULL,'S_pombe'),
 (7,'S. sodalis',5,'Soriculus sodalis','257447',NULL,'S._sodalis'),
 (8,'M_abscessus',NULL,'M_abscessus',NULL,NULL,'M_abscessus');
/*!40000 ALTER TABLE `Organism` ENABLE KEYS */;


--
-- Definition of table `SecurityGroup`
--

DROP TABLE IF EXISTS `SecurityGroup`;
CREATE TABLE `SecurityGroup` (
  `idSecurityGroup` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY  (`idSecurityGroup`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;



--
-- Definition of table `SecurityGroupCollaborator`
--

DROP TABLE IF EXISTS `SecurityGroupCollaborator`;
CREATE TABLE `SecurityGroupCollaborator` (
  `idSecurityGroup` int(10) unsigned NOT NULL,
  `idUser` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idSecurityGroup`,`idUser`),
  KEY `FK_SecurityGroupCollaborator_User` (`idUser`),
  CONSTRAINT `FK_SecurityGroupCollaborator_SecurityGroup` FOREIGN KEY (`idSecurityGroup`) REFERENCES `SecurityGroup` (`idSecurityGroup`),
  CONSTRAINT `FK_SecurityGroupCollaborator_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



--
-- Definition of table `SecurityGroupManager`
--

DROP TABLE IF EXISTS `SecurityGroupManager`;
CREATE TABLE `SecurityGroupManager` (
  `idSecurityGroup` int(10) unsigned NOT NULL,
  `idUser` int(10) unsigned NOT NULL auto_increment,
  PRIMARY KEY  (`idSecurityGroup`,`idUser`),
  KEY `FK_SecurityGroupManager_User` (`idUser`),
  CONSTRAINT `FK_SecurityGroupManager_SecurityGroup` FOREIGN KEY (`idSecurityGroup`) REFERENCES `SecurityGroup` (`idSecurityGroup`),
  CONSTRAINT `FK_SecurityGroupManager_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=latin1;



--
-- Definition of table `SecurityGroupMember`
--

DROP TABLE IF EXISTS `SecurityGroupMember`;
CREATE TABLE `SecurityGroupMember` (
  `idSecurityGroup` int(10) unsigned NOT NULL,
  `idUser` int(10) unsigned NOT NULL,
  PRIMARY KEY  USING BTREE (`idUser`,`idSecurityGroup`),
  KEY `FK_SecurityGroupUser_SecurityGroup` (`idSecurityGroup`),
  CONSTRAINT `FK_GroupUser_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_SecurityGroupUser_SecurityGroup` FOREIGN KEY (`idSecurityGroup`) REFERENCES `SecurityGroup` (`idSecurityGroup`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;


--
-- Definition of table `Segment`
--

DROP TABLE IF EXISTS `Segment`;
CREATE TABLE `Segment` (
  `idSegment` int(10) unsigned NOT NULL auto_increment,
  `length` int(10) unsigned NOT NULL,
  `name` varchar(100) NOT NULL,
  `idGenomeVersion` int(10) unsigned NOT NULL,
  `sortOrder` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idSegment`),
  KEY `FK_Segment_GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_Segment_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`)
) ENGINE=InnoDB AUTO_INCREMENT=220 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Segment`
--

/*!40000 ALTER TABLE `Segment` DISABLE KEYS */;
INSERT INTO `Segment` (`idSegment`,`length`,`name`,`idGenomeVersion`,`sortOrder`) VALUES 
 (1,247249719,'chr1',1,1),
 (2,242951149,'chr2',1,2),
 (3,199501827,'chr3',1,3),
 (4,191273063,'chr4',1,4),
 (5,180857866,'chr5',1,5),
 (6,170899992,'chr6',1,6),
 (7,158821424,'chr7',1,7),
 (8,146274826,'chr8',1,8),
 (9,140273252,'chr9',1,9),
 (10,135374737,'chr10',1,10),
 (11,134452384,'chr11',1,11),
 (12,132349534,'chr12',1,12),
 (13,114142980,'chr13',1,13),
 (14,106368585,'chr14',1,14),
 (15,100338915,'chr15',1,15),
 (16,88827254,'chr16',1,16),
 (17,78774742,'chr17',1,17),
 (18,76117153,'chr18',1,18),
 (19,63811651,'chr19',1,19),
 (20,62435964,'chr20',1,20),
 (21,46944323,'chr21',1,21),
 (22,49691432,'chr22',1,22),
 (23,154913754,'chrX',1,23),
 (24,57772954,'chrY',1,24),
 (25,16571,'chrM',1,25),
 (26,56204684,'chr1',5,1),
 (27,54366722,'chr2',5,2),
 (28,62931207,'chr3',5,3),
 (29,42602441,'chr4',5,4),
 (30,70371393,'chr5',5,5),
 (31,59200669,'chr6',5,6),
 (32,70262009,'chr7',5,7),
 (33,56456705,'chr8',5,8),
 (34,51490918,'chr9',5,9),
 (35,42379582,'chr10',5,10),
 (36,44616367,'chr11',5,11),
 (37,47523734,'chr12',5,12),
 (38,53547397,'chr13',5,13),
 (39,56522864,'chr14',5,14),
 (40,46629432,'chr15',5,15),
 (41,53070661,'chr16',5,16),
 (42,52310423,'chr17',5,17),
 (43,49281368,'chr18',5,18),
 (45,46181231,'chr19',5,19),
 (46,56528676,'chr20',5,20),
 (47,46057314,'chr21',5,21),
 (48,38981829,'chr22',5,22),
 (49,46388020,'chr23',5,23),
 (50,40293347,'chr24',5,24),
 (51,32876240,'chr25',5,25),
 (52,16596,'chrM',5,26),
 (53,122532868,'chrNA',5,27),
 (54,45965611,'chrScaffold',5,28),
 (55,15072419,'chrI',6,1),
 (56,15279316,'chrII',6,2),
 (57,13783681,'chrIII',6,3),
 (58,17493784,'chrIV',6,4),
 (59,20919398,'chrV',6,5),
 (60,13794,'chrM',6,6),
 (61,17718852,'chrX',6,7),
 (62,4171146,'chr',7,1),
 (63,52166,'phiSG1',7,2),
 (64,83307,'pSG1',7,3),
 (65,27241,'pSG2',7,4),
 (66,10811,'pSG3',7,5),
 (67,5579133,'chr1',4,1),
 (68,4539804,'chr2',4,2),
 (69,2452883,'chr3',4,3),
 (70,19431,'chrM',4,4),
 (71,35236,'mat',4,5),
 (188,247249719,'chr1',9,1),
 (189,242951149,'chr2',9,2),
 (190,199501827,'chr3',9,3),
 (191,191273063,'chr4',9,4),
 (192,180857866,'chr5',9,5),
 (193,170899992,'chr6',9,6),
 (194,158821424,'chr7',9,7),
 (195,146274826,'chr8',9,8),
 (196,140273252,'chr9',9,9),
 (197,135374737,'chr10',9,10),
 (198,134452384,'chr11',9,11),
 (199,132349534,'chr12',9,12),
 (200,114142980,'chr13',9,13),
 (201,106368585,'chr14',9,14),
 (202,100338915,'chr15',9,15),
 (203,88827254,'chr16',9,16),
 (204,78774742,'chr17',9,17),
 (205,76117153,'chr18',9,18),
 (206,63811651,'chr19',9,19),
 (207,62435964,'chr20',9,20),
 (208,46944323,'chr21',9,21),
 (209,49691432,'chr22',9,22),
 (210,154913754,'chrX',9,23),
 (211,57772954,'chrY',9,24),
 (212,16571,'chrM',9,25),
 (213,15072421,'chrI',11,1),
 (214,15279323,'chrII',11,2),
 (215,13783681,'chrIII',11,3),
 (216,17493785,'chrIV',11,4),
 (217,20919568,'chrV',11,5),
 (218,17718854,'chrX',11,6),
 (219,13794,'chrM',11,7);
/*!40000 ALTER TABLE `Segment` ENABLE KEYS */;


--
-- Definition of table `User`
--

DROP TABLE IF EXISTS `User`;
CREATE TABLE `User` (
  `idUser` int(10) unsigned NOT NULL auto_increment,
  `lastName` varchar(200) NOT NULL,
  `firstName` varchar(200) NOT NULL,
  `middleName` varchar(100) default NULL,
  `UserName` varchar(30) default NULL,
  `password` varchar(200) default NULL,
  PRIMARY KEY  USING BTREE (`idUser`),
  UNIQUE KEY `Index_UserName` (`UserName`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `User`
--

/*!40000 ALTER TABLE `User` DISABLE KEYS */;
INSERT INTO `User` (`idUser`,`lastName`,`firstName`,`middleName`,`UserName`,`password`) VALUES 
 (1,'','guest','','guest','454326b776dc46d32bb1050efe72df5e'),
 (2,'','admin','','admin','a447a70b58594f44a798d54cb4081fc2');


--
-- Definition of table `UserRole`
--

DROP TABLE IF EXISTS `UserRole`;
CREATE TABLE `UserRole` (
  `UserName` varchar(30) NOT NULL,
  `roleName` varchar(30) NOT NULL,
  `idUser` int(10) unsigned NOT NULL,
  `idUserRole` int(10) unsigned NOT NULL auto_increment,
  PRIMARY KEY  (`idUserRole`),
  KEY `FK_UserRole_Username` (`UserName`),
  KEY `FK_UserRole_User` (`idUser`),
  CONSTRAINT `FK_UserRole_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_UserRole_Username` FOREIGN KEY (`UserName`) REFERENCES `User` (`UserName`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `UserRole`
--

/*!40000 ALTER TABLE `UserRole` DISABLE KEYS */;
INSERT INTO `UserRole` (`UserName`,`roleName`,`idUser`,`idUserRole`) VALUES
 ('guest','das2guest',1,1),
 ('admin','das2admin',2,2);
/*!40000 ALTER TABLE `UserRole` ENABLE KEYS */;


--
-- Definition of table `Visibility`
--

DROP TABLE IF EXISTS `Visibility`;
CREATE TABLE `Visibility` (
  `codeVisibility` varchar(10) NOT NULL default '',
  `name` varchar(100) NOT NULL,
  PRIMARY KEY  (`codeVisibility`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Visibility`
--

/*!40000 ALTER TABLE `Visibility` DISABLE KEYS */;
INSERT INTO `Visibility` (`codeVisibility`,`name`) VALUES 
 ('MEM','Members'),
 ('MEMCOL','Members and Collaborators'),
 ('PUBLIC','Public');
/*!40000 ALTER TABLE `Visibility` ENABLE KEYS */;




/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;