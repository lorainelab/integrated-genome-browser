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
-- Create schema genopub
--

CREATE DATABASE IF NOT EXISTS genopub;
USE genopub;



--
-- Definition of table `Annotation`
--

DROP TABLE IF EXISTS `Annotation`;
CREATE TABLE `Annotation` (
  `idAnnotation` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(2000) NOT NULL,
  `description` varchar(10000) default NULL,
  `fileName` varchar(2000) default NULL,
  `idGenomeVersion` int(10) unsigned NOT NULL,
  `codeVisibility` varchar(10) NOT NULL,
  `idUser` int(10) unsigned default NULL,
  `idUserGroup` int(10) unsigned default NULL,
  `idInstitute` int(10) unsigned default NULL,
  `summary` varchar(5000) default NULL,
  `createdBy` varchar(200) default NULL,
  `createDate` datetime default NULL,  
  `isLoaded` char(1) default 'N',  
  PRIMARY KEY  (`idAnnotation`),
  KEY `FK_Annotation_GenomeVersion` (`idGenomeVersion`),
  KEY `FK_Annotation_User` (`idUser`),
  KEY `FK_Annotation_Visibility` (`codeVisibility`),
  KEY `FK_Annotation_group` USING BTREE (`idUserGroup`),
  KEY `FK_Annotation_Institute` USING BTREE (`idInstitute`),
  CONSTRAINT `FK_Annotation_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_Annotation_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_Annotation_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT FK_Annotation_Institute FOREIGN KEY FK_Annotation_Institute (idInstitute) REFERENCES genopub.Institute (idInstitute),
  CONSTRAINT `FK_Annotation_Visibility` FOREIGN KEY (`codeVisibility`) REFERENCES `Visibility` (`codeVisibility`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Annotation`
--

/*!40000 ALTER TABLE `Annotation` DISABLE KEYS */;
/*!40000 ALTER TABLE `Annotation` ENABLE KEYS */;

--
-- AnnotationCollaborator
--
DROP TABLE IF EXISTS genopub.AnnotationCollaborator;
CREATE TABLE genopub.AnnotationCollaborator (
  idAnnotation INT(10) unsigned  NOT NULL,
  idUser INT(10) unsigned  NOT NULL,
  PRIMARY KEY (idAnnotation, idUser),
  CONSTRAINT FK_AnnotationCollaborator_User FOREIGN KEY FK_AnnotationCollaborator_User (idUser)
    REFERENCES genopub.User (idUser)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT FK_AnnotationCollaborator_Annotation FOREIGN KEY FK_AnnotationCollaborator_Annotation (idAnnotation)
    REFERENCES genopub.Annotation (idAnnotation)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


--
-- Definition of table `AnnotationGrouping`
--

DROP TABLE IF EXISTS `AnnotationGrouping`;
CREATE TABLE `AnnotationGrouping` (
  `idAnnotationGrouping` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(2000) NOT NULL,
  `description` varchar(10000) default NULL,
  `idParentAnnotationGrouping` int(10) unsigned default NULL,
  `idGenomeVersion` int(10) unsigned  NOT NULL,
  `idUserGroup` int(10) unsigned default NULL,
  `createdBy` varchar(200) default NULL,
  `createDate` datetime default NULL,  
  PRIMARY KEY  USING BTREE (`idAnnotationGrouping`),
  KEY `FK_AnnotationGrouping_GenomeVersion` (`idGenomeVersion`),
  KEY `FK_AnnotationGrouping_parentAnnotationGrouping` USING BTREE (`idParentAnnotationGrouping`),
  KEY `FK_AnnotationGrouping_UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_AnnotationGrouping_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_AnnotationGrouping_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_AnnotationGrouping_parentAnnotationGrouping` FOREIGN KEY (`idParentAnnotationGrouping`) REFERENCES `AnnotationGrouping` (`idAnnotationGrouping`)
) ENGINE=InnoDB AUTO_INCREMENT=6300 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `AnnotationGrouping`
--

/*!40000 ALTER TABLE `AnnotationGrouping` DISABLE KEYS */;
INSERT INTO `AnnotationGrouping` (`idAnnotationGrouping`,`name`,`description`,`idParentAnnotationGrouping`,`idGenomeVersion`,`idUserGroup`) VALUES 
 (400,'A_thaliana_Jan_2004','A_thaliana_Jan_2004',NULL,400,NULL),
 (501,'B_taurus_Oct_2007','B_taurus_Oct_2007',NULL,501,NULL),
 (1202,'C_elegans_May_2007','C_elegans_May_2007',NULL,1202,NULL),
 (1204,'C_elegans_May_2008','C_elegans_May_2008',NULL,1204,NULL),
 (1500,'C_familiaris_May_2005','C_familiaris_May_2005',NULL,1500,NULL),
 (1900,'D_melanogaster_Apr_2004','D_melanogaster_Apr_2004',NULL,1900,NULL),
 (1901,'D_melanogaster_Apr_2006','D_melanogaster_Apr_2006',NULL,1901,NULL),
 (2700,'D_rerio_Mar_2006','D_rerio_Mar_2006',NULL,2700,NULL),
 (2701,'D_rerio_Jul_2007','D_rerio_Jul_2007',NULL,2701,NULL),
 (2900,'E_coli_Oct_2007','E_coli_Oct_2007',NULL,2900,NULL),
 (3100,'G_gallus_May_2006','G_gallus_May_2006',NULL,3100,NULL),
 (3404,'H_sapiens_May_2004','H_sapiens_May_2004',NULL,3404,NULL),
 (3405,'H_sapiens_Mar_2006','H_sapiens_Mar_2006',NULL,3405,NULL),
 (3500,'M_mulatta_Jan_2006','M_mulatta_Jan_2006',NULL,3500,NULL),
 (3600,'M_truncatula_Aug_2007','M_truncatula_Aug_2007',NULL,3600,NULL),
 (3804,'M_musculus_Mar_2006','M_musculus_Mar_2006',NULL,3804,NULL),
 (3806,'M_musculus_Jul_2007','M_musculus_Jul_2007',NULL,3806,NULL),
 (3900,'M_abscessus_Mar_2008','M_abscessus_Mar_2008',NULL,3900,NULL),
 (4000,'M_smegmatis_Mar_2008','M_smegmatis_Mar_2008',NULL,4000,NULL),
 (4100,'M_tuberculosis_Sep_2008','M_tuberculosis_Sep_2008',NULL,4100,NULL),
 (4302,'O_sativa_Jun_2009','O_sativa_Jun_2009',NULL,4302,NULL),
 (4800,'P_falciparum_Jul_2007','P_falciparum_Jul_2007',NULL,4800,NULL),
 (5000,'P_trichocarpa_Jun_2004','P_trichocarpa_Jun_2004',NULL,5000,NULL),
 (5202,'R_norvegicus_Nov_2004','R_norvegicus_Nov_2004',NULL,5202,NULL),
 (5306,'S_cerevisiae_Apr_2008','S_cerevisiae_Apr_2008',NULL,5306,NULL),
 (5400,'S_pombe_Sep_2004','S_pombe_Sep_2004',NULL,5400,NULL),
 (5401,'S_pombe_Apr_2007','S_pombe_Apr_2007',NULL,5401,NULL),
 (5500, 'S_glossinidius_Jan_2006', 'S_glossinidius_Jan_2006', NULL, 5500, NULL),
 (6000,'T_nigroviridis_Feb_2004','T_nigroviridis_Feb_2004',NULL,6000,NULL),
 (6100,'V_vinifera_Apr_2007','V_vinifera_Apr_2007',NULL,6100,NULL),
 (6201,'X_tropicalis_Aug_2005','X_tropicalis_Aug_2005',NULL,6201,NULL),
 (6202,'H_sapiens_Feb_2009','H_sapiens_Feb_2009',NULL,6202,NULL);
/*!40000 ALTER TABLE `AnnotationGrouping` ENABLE KEYS */;


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
-- Dumping data for table `AnnotationToAnnotationGrouping`
--

/*!40000 ALTER TABLE `AnnotationToAnnotationGrouping` DISABLE KEYS */;
/*!40000 ALTER TABLE `AnnotationToAnnotationGrouping` ENABLE KEYS */;


--
-- Definition of table `GenomeVersion`
--

DROP TABLE IF EXISTS `GenomeVersion`;
CREATE TABLE `GenomeVersion` (
  `idGenomeVersion` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(50) NOT NULL,
  `ucscName` varchar(100) default NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=6300 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `GenomeVersion`
--

/*!40000 ALTER TABLE `GenomeVersion` DISABLE KEYS */;
INSERT INTO `GenomeVersion` (`idGenomeVersion`,`name`,`idOrganism`,`buildDate`,`coordURI`,`coordVersion`,`coordSource`,`coordTestRange`,`coordAuthority`,`ucscName`) VALUES 
 (400,'A_thaliana_Jan_2004',400,'2004-01-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (501,'B_taurus_Oct_2007',500,'2007-10-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'bosTau4'),
 (1202,'C_elegans_Jan_2007',1200,'2007-01-01 00:00:00','http://www.wormbase.org/genome/C_elegans/WS180/','180','Chromosome','','WS','ce4'),
 (1204,'C_elegans_May_2008',1200,'2008-05-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'ce6'),
 (1500,'C_familiaris_May_2005',1500,'2005-05-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (1900,'D_melanogaster_Apr_2004',1900,'2004-04-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'dm2'),
 (1901,'D_melanogaster_Apr_2006',1900,'2006-04-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'dm3'),
 (2700,'D_rerio_Mar_2006',2700,'2006-03-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'danRer6'),
 (2701,'D_rerio_Jul_2007',2700,'2007-07-01 00:00:00','http://zfin.org/genome/D_rerio/Zv7/','Zv7','Chromosome',NULL,'ZFISH_7','danRer7'),
 (2900,'E_coli_Oct_2007',2900,'2007-10-01 00:00:00','','','','','',NULL),
 (3100,'G_gallus_May_2006',3100,'2006-05-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'galGal3'),
 (3404,'H_sapiens_May_2004',3400,'2004-05-01 00:00:00','http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B35.1/','35','Chromosome',NULL,'NCBI','hg17'),
 (3405,'H_sapiens_Mar_2006',3400,'2006-03-01 00:00:00','http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B36.1/','36','Chromosome','','NCBI','hg18'),
 (3500,'M_mulatta_Jan_2006',3500,'2006-01-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (3600,'M_truncatula_Aug_2007',3600,'2007-08-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (3804,'M_musculus_Mar_2006',3800,'2006-03-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'mm8'),
 (3806,'M_musculus_Jul_2007',3800,'2007-07-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'mm9'),
 (3900,'M_abscessus_Mar_2008',3900,'2008-03-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (4000,'M_smegmatis_Mar_2008',4000,'2008-03-01 00:00:00','','','','','',NULL),
 (4100,'M_tuberculosis_Sep_2008',4100,'2008-09-01 00:00:00','','','','','',NULL),
 (4302,'O_sativa_Jun_2009',4300,'2009-06-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (4800,'P_falciparum_Jul_2007',4800,'2007-07-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (5000,'P_trichocarpa_Jun_2004',5000,'2004-06-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (5202,'R_norvegicus_Nov_2004',5200,'2004-11-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'rn4'),
 (5306,'S_cerevisiae_Apr_2008',5300,'2008-04-01 00:00:00',NULL,NULL,NULL,NULL,NULL,'sacCer2'),
 (5400,'S_pombe_Sep_2004',5400,'2004-09-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (5401,'S_pombe_Apr_2007',5400,'2007-04-01 00:00:00','http://www.sanger.ac.uk/Projects/S_pombe/Apr_2007','Apr_2007','Chromosome','','Sanger',NULL),
 (5500,'S_glossinidius_Jan_2006',5500,'2006-01-01 00:00:00','ftp://ftp.ncbi.nih.gov/genomes/Bacteria/Sodalis_glossinidius_morsitans/Jan_2006','Jan_2006','Chromosome',NULL,'NCBI',NULL),
 (6000,'T_nigroviridis_Feb_2004',6000,'2004-02-01 00:00:00','','','','','',NULL),
 (6100,'V_vinifera_Apr_2007',6100,'2007-04-01 00:00:00',NULL,NULL,NULL,NULL,NULL,NULL),
 (6201,'X_tropicalis_Aug_2005',6200,'2005-08-01 00:00:00','','','','','','xenTro2'),
 (6202,'H_sapiens_Feb_2009',3400,'2009-02-01 00:00:00','','','','','','hg19');
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
  UNIQUE KEY `Index_OrganismName` (`name`),
  UNIQUE KEY `Index_OrganismBinomialName` (`binomialName`)
) ENGINE=InnoDB AUTO_INCREMENT=6300 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Organism`
--

/*!40000 ALTER TABLE `Organism` DISABLE KEYS */;
INSERT INTO `Organism` (`idOrganism`,`commonName`,`sortOrder`,`binomialName`,`NCBITaxID`,`idUser`,`name`) VALUES 
 (100,'Lizard',NULL,'Anolis carolinensis',NULL,NULL,'A_carolinensis'),
 (200,'A.gambiae',NULL,'Anopheles gambiae',NULL,NULL,'A_gambiae'),
 (300,'A_mellifera',NULL,'Apis mellifera','',NULL,'A_mellifera'),
 (400,'A. thaliana',NULL,'Arabidopsis thaliana',NULL,NULL,'A_thaliana'),
 (500,'Cow',NULL,'Bos taurus',NULL,NULL,'B_taurus'),
 (600,'Lancet',NULL,'Branchiostoma floridae',NULL,NULL,'B_floridae'),
 (700,'Marmoset',NULL,'Callithrix jacchus',NULL,NULL,'C_jacchus'),
 (800,'Guinea Pig',NULL,'Cavia porcellus',NULL,NULL,'C_porcellus'),
 (900,'C. intestinalis',NULL,'Ciona intestinalis',NULL,NULL,'C_intestinalis'),
 (1000,'C. brenneri',NULL,'Caenorhabditis brenneri',NULL,NULL,'C_brenneri'),
 (1100,'C. briggsae',NULL,'Caenorhabditis briggsae',NULL,NULL,'C_briggsae'),
 (1200,'C. elegans',NULL,'Caenorhabditis elegans','6239',NULL,'C_elegans'),
 (1300,'C. japonica',NULL,'Caenorhabditis japonica',NULL,NULL,'C_japonica'),
 (1400,'C. remanei',NULL,'Caenorhabditis remanei',NULL,NULL,'C_remanei'),
 (1500,'Dog',NULL,'Canis lupus familiaris',NULL,NULL,'C_familiaris'),
 (1600,'D. ananassae',NULL,'Drosophila ananassae',NULL,NULL,'D_ananassae'),
 (1700,'D. erecta',NULL,'Drosophila erecta',NULL,NULL,'D_erecta'),
 (1800,'D. grimshawi',NULL,'Drosophila grimshawi',NULL,NULL,'D_grimshawi'),
 (1900,'D. melanogaster',NULL,'Drosophila melanogaster',NULL,NULL,'D_melanogaster'),
 (2000,'D. mojavensis',NULL,'Drosophila mojavensis',NULL,NULL,'D_mojavensis'),
 (2100,'D. persimilis',NULL,'Drosophila persimilis',NULL,NULL,'D_persimilis'),
 (2200,'D. pseudoobscura',NULL,'Drosophila pseudoobscura',NULL,NULL,'D_pseudoobscura'),
 (2300,'D. sechellia',NULL,'Drosophila sechellia',NULL,NULL,'D_sechellia'),
 (2400,'D. simulans',NULL,'Drosophila simulans',NULL,NULL,'D_simulans'),
 (2500,'D. virilis',NULL,'Drosophila virilis',NULL,NULL,'D_virilis'),
 (2600,'D. yakuba',NULL,'Drosophila yakuba',NULL,NULL,'D_yakuba'),
 (2700,'Zebrafish',NULL,'Danio rerio','7955',NULL,'D_rerio'),
 (2800,'Horse',NULL,'Equus caballus',NULL,NULL,'E_caballus'),
 (2900,'E. coli',NULL,'Escherichia coli',NULL,NULL,'E_coli'),
 (3000,'Cat',NULL,'Felis catus',NULL,NULL,'F_catus'),
 (3100,'Chicken',NULL,'Gallus gallus',NULL,NULL,'G_gallus'),
 (3200,'Stickleback G_aculeatus',NULL,'Gasterosteus aculeatus','',NULL,'G_aculeatus'),
 (3300,'Soybean',NULL,'Glycine max',NULL,NULL,'G_max'),
 (3400,'Human',NULL,'Homo sapiens','9606',NULL,'H_sapiens'),
 (3500,'Rhesus',NULL,'Macaca mulatta',NULL,NULL,'M_mulatta'),
 (3600,'Barrel Medic',NULL,'Medicago truncatula',NULL,NULL,'M_truncatula'),
 (3700,'Opossum',NULL,'Monodelphis domestica',NULL,NULL,'M_domestica'),
 (3800,'Mouse',NULL,'Mus musculus','10090',NULL,'M_musculus'),
 (3900,'M. abcessus',NULL,'Mycobacterium abscessus',NULL,NULL,'M_abscessus'),
 (4000,'M. smegmatis',NULL,'Mycobacterium smegmatis',NULL,NULL,'M_smegmatis'),
 (4100,'M. tuberculosis',NULL,'Mycobacterium tuberculosis',NULL,NULL,'M_tuberculosis'),
 (4200,'Platypus',NULL,'Ornithorhynchus anatinus',NULL,NULL,'O_anatinus'),
 (4300,'Rice',NULL,'Oryza sativa',NULL,NULL,'O_sativa'),
 (4400,'Medaka',NULL,'Oryzias latipes',NULL,NULL,'O_latipes'),
 (4500,'O. lucimarinus',NULL,'Ostreococcus lucimarinus',NULL,NULL,'O_lucimarinus_Apr_2007'),
 (4600,'Chimp',NULL,'Pan troglodytes',NULL,NULL,'P_troglodytes'),
 (4700,'Lamprey',NULL,'Petromyzon marinus',NULL,NULL,'P_marinus'),
 (4800,'P. falciparum',NULL,'Plasmodium falciparum',NULL,NULL,'P_falciparum'),
 (4900,'Orangutan',NULL,'Pongo pygmaeus abelii',NULL,NULL,'P_abelii'),
 (5000,'Black Cottonwood',NULL,'Populus trichocarpa',NULL,NULL,'P_trichocarpa'),
 (5100,'P. pacificus',NULL,'Pristionchus pacificus',NULL,NULL,'P_pacificus'),
 (5200,'Rat',NULL,'Rattus norvegicus',NULL,NULL,'R_norvegicus'),
 (5300,'Yeast',NULL,'Saccharomyces cerevisiae',NULL,NULL,'S_cerevisiae'),
 (5400,'Fission Yeast',NULL,'Schizosaccharomyces pombe','4896',NULL,'S_pombe'),
 (5500,'S. glossinidius',NULL,'Sodalis glossinidius','343509',NULL,'S_glossinidius'),
 (5600,'Sorghum',NULL,'Sorghum bicolor','',NULL,'S_bicolor'),
 (5700,'Purple Sea Urchin',NULL,'Strongylocentrotus purpuratus',NULL,NULL,'S_purpuratus'),
 (5800,'Zebra Finch',NULL,'Taeniopygia guttata','',NULL,'T_guttata'),
 (5900,'Fugu',NULL,'Takifugu rubripes','',NULL,'T_rubripes'),
 (6000,'Tetraodon',NULL,'Tetraodon nigroviridis','',NULL,'T_nigroviridis'),
 (6100,'Common Grape Vine',NULL,'Vitis vinifera',NULL,NULL,'V_vinifera'),
 (6200,'Pipid Frog',NULL,'Xenopus tropicalis','',NULL,'X_tropicalis');
/*!40000 ALTER TABLE `Organism` ENABLE KEYS */;



--
-- Definition of table `UnloadAnnotation`
--
DROP TABLE IF EXISTS `UnloadAnnotation`;
CREATE TABLE  `UnloadAnnotation` (
  `idUnloadAnnotation` int(10) unsigned NOT NULL auto_increment,
  `typeName` varchar(2000) NOT NULL,
  `idUser` int(10) unsigned default NULL,
  `idGenomeVersion` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idUnloadAnnotation`),
  KEY `FK_UnloadAnnotation_User` (`idUser`),
  KEY `FK_UnloadAnnotation_GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_UnloadAnnotation_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_UnloadAnnotation_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;

--
-- Definition of table `UserGroup`
--

DROP TABLE IF EXISTS `UserGroup`;
CREATE TABLE `UserGroup` (
  `idUserGroup` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(200) NOT NULL,
  `contact` varchar(500) default NULL,
  `email` varchar(500) default NULL,
  `institute` varchar(200) default NULL,  
  PRIMARY KEY  (`idUserGroup`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `UserGroup`
--

/*!40000 ALTER TABLE `UserGroup` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserGroup` ENABLE KEYS */;


--
-- Definition of table `UserGroupCollaborator`
--

DROP TABLE IF EXISTS `UserGroupCollaborator`;
CREATE TABLE `UserGroupCollaborator` (
  `idUserGroup` int(10) unsigned NOT NULL,
  `idUser` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idUserGroup`,`idUser`),
  KEY `FK_UserGroupCollaborator_User` (`idUser`),
  CONSTRAINT `FK_UserGroupCollaborator_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_UserGroupCollaborator_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `UserGroupCollaborator`
--

/*!40000 ALTER TABLE `UserGroupCollaborator` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserGroupCollaborator` ENABLE KEYS */;


--
-- Definition of table `UserGroupManager`
--

DROP TABLE IF EXISTS `UserGroupManager`;
CREATE TABLE `UserGroupManager` (
  `idUserGroup` int(10) unsigned NOT NULL,
  `idUser` int(10) unsigned NOT NULL auto_increment,
  PRIMARY KEY  (`idUserGroup`,`idUser`),
  KEY `FK_UserGroupManager_User` (`idUser`),
  CONSTRAINT `FK_UserGroupManager_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_UserGroupManager_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `UserGroupManager`
--

/*!40000 ALTER TABLE `UserGroupManager` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserGroupManager` ENABLE KEYS */;


--
-- Definition of table `UserGroupMember`
--

DROP TABLE IF EXISTS `UserGroupMember`;
CREATE TABLE `UserGroupMember` (
  `idUserGroup` int(10) unsigned NOT NULL,
  `idUser` int(10) unsigned NOT NULL,
  PRIMARY KEY  USING BTREE (`idUser`,`idUserGroup`),
  KEY `FK_UserGroupUser_UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_UserGroupMember_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_UserGroupMember_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;

--
-- Dumping data for table `UserGroupMember`
--

/*!40000 ALTER TABLE `UserGroupMember` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserGroupMember` ENABLE KEYS */;


-- Add new Table Institute
DROP TABLE IF EXISTS `genopub`.`Institute`;
CREATE  TABLE `genopub`.`Institute` (
  `idInstitute` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT ,
  `name` VARCHAR(200) NOT NULL ,
  `description` VARCHAR(500) NULL ,
  `isActive` CHAR(1) NULL DEFAULT 'Y' ,
  PRIMARY KEY (`idInstitute`) ) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;

    
    
-- Add new Table to link UserGroup to multiple Institutes
DROP TABLE IF EXISTS `genopub`.`InstituteUserGroup`;
CREATE TABLE `genopub`.`InstituteUserGroup` ( 
    `idInstitute`	 INT(10) unsigned,
    `idUserGroup`            INT(10) unsigned,
    PRIMARY KEY (`idInstitute`, `idUserGroup`),
CONSTRAINT `FK_InstituteUserGroup_Institute` FOREIGN KEY `FK_InstituteUserGroup_Institute` (idInstitute)
    REFERENCES genopub.Institute (`idInstitute`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
CONSTRAINT `FK_InstituteUserGroup_UserGroup` FOREIGN KEY `FK_InstituteUserGroup_UserGroup` (idUserGroup)
    REFERENCES genopub.UserGroup (`idUserGroup`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;

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
 (1,247249719,'chr1',3405,1),
 (2,242951149,'chr2',3405,2),
 (3,199501827,'chr3',3405,3),
 (4,191273063,'chr4',3405,4),
 (5,180857866,'chr5',3405,5),
 (6,170899992,'chr6',3405,6),
 (7,158821424,'chr7',3405,7),
 (8,146274826,'chr8',3405,8),
 (9,140273252,'chr9',3405,9),
 (10,135374737,'chr10',3405,10),
 (11,134452384,'chr11',3405,11),
 (12,132349534,'chr12',3405,12),
 (13,114142980,'chr13',3405,13),
 (14,106368585,'chr14',3405,14),
 (15,100338915,'chr15',3405,15),
 (16,88827254,'chr16',3405,16),
 (17,78774742,'chr17',3405,17),
 (18,76117153,'chr18',3405,18),
 (19,63811651,'chr19',3405,19),
 (20,62435964,'chr20',3405,20),
 (21,46944323,'chr21',3405,21),
 (22,49691432,'chr22',3405,22),
 (23,154913754,'chrX',3405,23),
 (24,57772954,'chrY',3405,24),
 (25,16571,'chrM',3405,25),
 (26,56204684,'chr1',2701,1),
 (27,54366722,'chr2',2701,2),
 (28,62931207,'chr3',2701,3),
 (29,42602441,'chr4',2701,4),
 (30,70371393,'chr5',2701,5),
 (31,59200669,'chr6',2701,6),
 (32,70262009,'chr7',2701,7),
 (33,56456705,'chr8',2701,8),
 (34,51490918,'chr9',2701,9),
 (35,42379582,'chr10',2701,10),
 (36,44616367,'chr11',2701,11),
 (37,47523734,'chr12',2701,12),
 (38,53547397,'chr13',2701,13),
 (39,56522864,'chr14',2701,14),
 (40,46629432,'chr15',2701,15),
 (41,53070661,'chr16',2701,16),
 (42,52310423,'chr17',2701,17),
 (43,49281368,'chr18',2701,18),
 (45,46181231,'chr19',2701,19),
 (46,56528676,'chr20',2701,20),
 (47,46057314,'chr21',2701,21),
 (48,38981829,'chr22',2701,22),
 (49,46388020,'chr23',2701,23),
 (50,40293347,'chr24',2701,24),
 (51,32876240,'chr25',2701,25),
 (52,16596,'chrM',2701,26),
 (53,122532868,'chrNA',2701,27),
 (54,45965611,'chrScaffold',2701,28),
 (55,15072419,'chrI',5401,1),
 (56,15279316,'chrII',5401,2),
 (57,13783681,'chrIII',5401,3),
 (58,17493784,'chrIV',5401,4),
 (59,20919398,'chrV',5401,5),
 (60,13794,'chrM',5401,6),
 (61,17718852,'chrX',5401,7),
 (62,4171146,'chr',5500,1),
 (63,52166,'phiSG1',5500,2),
 (64,83307,'pSG1',5500,3),
 (65,27241,'pSG2',5500,4),
 (66,10811,'pSG3',5500,5),
 (67,5579133,'chr1',5401,1),
 (68,4539804,'chr2',5401,2),
 (69,2452883,'chr3',5401,3),
 (70,19431,'chrM',5401,4),
 (71,35236,'mat',5401,5),
 (188,247249719,'chr1',3404,1),
 (189,242951149,'chr2',3404,2),
 (190,199501827,'chr3',3404,3),
 (191,191273063,'chr4',3404,4),
 (192,180857866,'chr5',3404,5),
 (193,170899992,'chr6',3404,6),
 (194,158821424,'chr7',3404,7),
 (195,146274826,'chr8',3404,8),
 (196,140273252,'chr9',3404,9),
 (197,135374737,'chr10',3404,10),
 (198,134452384,'chr11',3404,11),
 (199,132349534,'chr12',3404,12),
 (200,114142980,'chr13',3404,13),
 (201,106368585,'chr14',3404,14),
 (202,100338915,'chr15',3404,15),
 (203,88827254,'chr16',3404,16),
 (204,78774742,'chr17',3404,17),
 (205,76117153,'chr18',3404,18),
 (206,63811651,'chr19',3404,19),
 (207,62435964,'chr20',3404,20),
 (208,46944323,'chr21',3404,21),
 (209,49691432,'chr22',3404,22),
 (210,154913754,'chrX',3404,23),
 (211,57772954,'chrY',3404,24),
 (212,16571,'chrM',3404,25),
 (213,15072421,'chrI',1204,1),
 (214,15279323,'chrII',1204,2),
 (215,13783681,'chrIII',1204,3),
 (216,17493785,'chrIV',1204,4),
 (217,20919568,'chrV',1204,5),
 (218,17718854,'chrX',1204,6),
 (219,13794,'chrM',1204,7);
 
INSERT INTO `Segment` (`name`,`length`,`sortOrder`,  `idGenomeVersion`) VALUES 
('chr1',	249250621,	1, 6202),
('chr2',	243199373,	2, 6202),
('chr3',	198022430,	3, 6202),
('chr4',	191154276,	4, 6202),
('chr5',	180915260,	5, 6202),
('chr6',	171115067,	6, 6202),
('chr7',	159138663,	7, 6202),
('chr8',	146364022,	8, 6202),
('chr9',	141213431,	9, 6202),
('chr10',	135534747,	10, 6202),
('chr11',	135006516,	11, 6202),
('chr12',	133851895,	12, 6202),
('chr13',	115169878,	13, 6202),
('chr14',	107349540,	14, 6202),
('chr15',	102531392,	15, 6202),
('chr16',	90354753,	16, 6202),
('chr17',	81195210,	17, 6202),
('chr18',	78077248,	18, 6202),
('chr19',	59128983,	19, 6202),
('chr20',	63025520,	20, 6202),
('chr21',	48129895,	21, 6202),
('chr22',	51304566,	22, 6202),
('chrX',	155270560,	23, 6202),
('chrY',	59373566,	24, 6202),
('chrM',	16571,	25, 6202),
('chr4_ctg9_hap1',	590426,	26, 6202),
('chr6_apd_hap1',	4622290,	27, 6202),
('chr6_cox_hap2',	4795371,	28, 6202),
('chr6_dbb_hap3',	4610396,	29, 6202),
('chr6_mann_hap4',	4683263,	30, 6202),
('chr6_mcf_hap5',	4833398,	31, 6202),
('chr6_qbl_hap6',	4611984,	32, 6202),
('chr6_ssto_hap7',	4928567,	33, 6202),
('chr17_ctg5_hap1',	1680828,	34, 6202),
('chr1_gl000191_random',	106433,	35, 6202),
('chr1_gl000192_random',	547496,	36, 6202),
('chr4_gl000193_random',	189789,	37, 6202),
('chr4_gl000194_random',	191469,	38, 6202),
('chr7_gl000195_random',	182896,	39, 6202),
('chr8_gl000196_random',	38914,	40, 6202),
('chr8_gl000197_random',	37175,	41, 6202),
('chr9_gl000198_random',	90085,	42, 6202),
('chr9_gl000199_random',	169874,	43, 6202),
('chr9_gl000200_random',	187035,	44, 6202),
('chr9_gl000201_random',	36148,	45, 6202),
('chr11_gl000202_random',	40103,	46, 6202),
('chr17_gl000203_random',	37498,	47, 6202),
('chr17_gl000204_random',	81310,	48, 6202),
('chr17_gl000205_random',	174588,	49, 6202),
('chr17_gl000206_random',	41001,	50, 6202),
('chr18_gl000207_random',	4262,	51, 6202),
('chr19_gl000208_random',	92689,	52, 6202),
('chr19_gl000209_random',	159169,	53, 6202),
('chr21_gl000210_random',	27682,	54, 6202),
('chrUn_gl000211',	166566,	55, 6202),
('chrUn_gl000212',	186858,	56, 6202),
('chrUn_gl000213',	164239,	57, 6202),
('chrUn_gl000214',	137718,	58, 6202),
('chrUn_gl000215',	172545,	59, 6202),
('chrUn_gl000216',	172294,	60, 6202),
('chrUn_gl000217',	172149,	61, 6202),
('chrUn_gl000218',	161147,	62, 6202),
('chrUn_gl000219',	179198,	63, 6202),
('chrUn_gl000220',	161802,	64, 6202),
('chrUn_gl000221',	155397,	65, 6202),
('chrUn_gl000222',	186861,	66, 6202),
('chrUn_gl000223',	180455,	67, 6202),
('chrUn_gl000224',	179693,	68, 6202),
('chrUn_gl000225',	211173,	69, 6202),
('chrUn_gl000226',	15008,	70, 6202),
('chrUn_gl000227',	128374,	71, 6202),
('chrUn_gl000228',	129120,	72, 6202),
('chrUn_gl000229',	19913,	73, 6202),
('chrUn_gl000230',	43691,	74, 6202),
('chrUn_gl000231',	27386,	75, 6202),
('chrUn_gl000232',	40652,	76, 6202),
('chrUn_gl000233',	45941,	77, 6202),
('chrUn_gl000234',	40531,	78, 6202),
('chrUn_gl000235',	34474,	79, 6202),
('chrUn_gl000236',	41934,	80, 6202),
('chrUn_gl000237',	45867,	81, 6202),
('chrUn_gl000238',	39939,	82, 6202),
('chrUn_gl000239',	33824,	83, 6202),
('chrUn_gl000240',	41933,	84, 6202),
('chrUn_gl000241',	42152,	85, 6202),
('chrUn_gl000242',	43523,	86, 6202),
('chrUn_gl000243',	43341,	87, 6202),
('chrUn_gl000244',	39929,	88, 6202),
('chrUn_gl000245',	36651,	89, 6202),
('chrUn_gl000246',	38154,	90, 6202),
('chrUn_gl000247',	36422,	91, 6202),
('chrUn_gl000248',	39786,	92, 6202),
('chrUn_gl000249',	38502,	93, 6202);
 
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
  `email` varchar(500) default NULL,
  `institute` varchar(200) default NULL,
  `UserName` varchar(30) default NULL,
  `password` varchar(200) default NULL,
  `ucscUrl` varchar(250) default 'http://genome.ucsc.edu',
  PRIMARY KEY  USING BTREE (`idUser`),
  UNIQUE KEY `Index_UserName` (`UserName`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `User`
--

/*!40000 ALTER TABLE `User` DISABLE KEYS */;
INSERT INTO `User` (`idUser`,`lastName`,`firstName`,`middleName`,`UserName`,`password`) VALUES 
 (1,'','Guest','','guest','454326b776dc46d32bb1050efe72df5e'),
 (2,'','Admin','','admin','a447a70b58594f44a798d54cb4081fc2'),
 (3,'','Visitor','','','9d9db6877f2837c5e5d5d31102377213');
/*!40000 ALTER TABLE `User` ENABLE KEYS */;


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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `UserRole`
--

/*!40000 ALTER TABLE `UserRole` DISABLE KEYS */;
INSERT INTO `UserRole` (`UserName`,`roleName`,`idUser`,`idUserRole`) VALUES 
 ('guest','guest',1,1),
 ('admin','admin',2,2),
 ('','guest',3,3);
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
 ('OWNER','Owner'),
 ('MEM','Members'),
 ('INST','Institute'),
 ('PUBLIC','Public');
/*!40000 ALTER TABLE `Visibility` ENABLE KEYS */;


-- Add new Table PropertyType
DROP TABLE IF EXISTS PropertyType;
CREATE  TABLE genopub.PropertyType (
  codePropertyType VARCHAR(10) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  isActive CHAR(1) NULL DEFAULT 'Y' ,
PRIMARY KEY (codePropertyType)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;




--
-- Dumping data for table `propertytype`
--

/*!40000 ALTER TABLE `PropertyType` DISABLE KEYS */;
INSERT INTO `PropertyType` VALUES 
('CHECK','Checkbox','Y'),
('MOPTION','Option (Multiple selection)','Y'),
('OPTION','Option (Single selection)','Y'),
('TEXT','Text','Y'),('URL','URL','Y');
/*!40000 ALTER TABLE `PropertyType` ENABLE KEYS */;



-- Add new Table Property
DROP TABLE IF EXISTS Property;
CREATE  TABLE genopub.Property (
  idProperty INT(10) UNSIGNED NOT NULL AUTO_INCREMENT ,
  name VARCHAR(200) NOT NULL ,
  isActive CHAR(1) NULL DEFAULT 'Y' ,
  codePropertyType VARCHAR(10) NOT NULL,
  `idUser` int(10) unsigned default NULL,
  sortOrder INT(10) UNSIGNED NULL,
  PRIMARY KEY (idProperty),
  KEY `FK_Property_User` (`idUser`),
  KEY `FK_Property_PropertyType` (`codePropertyType`),
  CONSTRAINT `FK_Property_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_Property_PropertyType` FOREIGN KEY (`codePropertyType`) REFERENCES `PropertyType` (`codePropertyType`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;



--
-- Dumping data for table `property`
--


/*!40000 ALTER TABLE `Property` DISABLE KEYS */;
INSERT INTO `Property` VALUES 
(19,'Analysis Type','Y','OPTION',NULL, 1),
(22,'Experiment Platform','Y','OPTION',NULL, 2),
(23,'Experiment Method','Y','OPTION',NULL, 3);
/*!40000 ALTER TABLE `Property` ENABLE KEYS */;



-- Add new Table PropertyOption
DROP TABLE IF EXISTS PropertyOption;
CREATE  TABLE genopub.PropertyOption (
  idPropertyOption INT(10) UNSIGNED NOT NULL AUTO_INCREMENT ,
  name VARCHAR(200) NOT NULL ,
  isActive CHAR(1) NULL DEFAULT 'Y' ,
  idProperty INT(10) UNSIGNED NOT NULL,
  sortOrder INT(10) UNSIGNED NULL,
  PRIMARY KEY (idPropertyOption),
  KEY `FK_PropertyOption_Property` (`idProperty`),
  CONSTRAINT `FK_PropertyOption_Property` FOREIGN KEY (`idProperty`) REFERENCES `Property` (`idProperty`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;

--
-- Dumping data for table `propertyoption`
--


/*!40000 ALTER TABLE `PropertyOption` DISABLE KEYS */;
INSERT INTO `PropertyOption` VALUES 
(32,'','Y',19,0),
(33,'Dynamic/Differential','Y',19,1),
(34,'Static','Y',19,2),
(52,'','Y',22,0),
(53,'Illumina Genome Analyzer','Y',22,1),
(54,'Agilent Microarray','Y',22,2),
(55,'Affymetrix Microarray','Y',22,3),
(56,'454 Genome Sequencer','Y',22,4),
(57,'Applied Biosystems SOLiD','Y',22,5),
(58,'Helicos Genomic Signature Sequencing','Y',22,6),(60,'','Y',23,0),
(61,'chIP-seq','Y',23,1),
(62,'chIP-chip','Y',23,2),
(63,'CGN Microarray','Y',23,3),
(64,'SNP Microarray','Y',23,4),
(65,'Transcriptome Microarray','Y',23,5),
(66,'Gene Expression Microarray','Y',23,6),
(67,'Quantitative rtPCR','Y',23,7),
(68,'SAGE','Y',23,8);
/*!40000 ALTER TABLE `PropertyOption` ENABLE KEYS */;



-- Add new Table AnnotationProperty
DROP TABLE IF EXISTS AnnotationProperty;
CREATE  TABLE genopub.AnnotationProperty (
  idAnnotationProperty INT(10) UNSIGNED NOT NULL AUTO_INCREMENT ,
  name VARCHAR(200) NOT NULL ,
  value VARCHAR(200)  NULL ,
  idProperty INT(10) UNSIGNED NOT NULL,
  idAnnotation INT(10) UNSIGNED NOT NULL,
  PRIMARY KEY (idAnnotationProperty),
  KEY `FK_AnnotationProperty_Property` (`idProperty`),
  KEY `FK_AnnotationProperty_Annotation` (`idAnnotation`),
  CONSTRAINT `FK_AnnotationProperty_Property` FOREIGN KEY (`idProperty`) REFERENCES `Property` (`idProperty`),
  CONSTRAINT `FK_AnnotationProperty_Annotation` FOREIGN KEY (`idAnnotation`) REFERENCES `Annotation` (`idAnnotation`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;

-- Add new Table AnnotationPropertyOption
DROP TABLE IF EXISTS AnnotationPropertyOption;
CREATE TABLE genopub.AnnotationPropertyOption (
  idAnnotationProperty INT(10) unsigned  NOT NULL,
  idPropertyOption INT(10) unsigned  NOT NULL,
  PRIMARY KEY (idAnnotationProperty, idPropertyOption),
  CONSTRAINT FK_AnnotationPropertyOption_AnnotationProperty FOREIGN KEY FK_AnnotationPropertyOption_AnnotationProperty (idAnnotationProperty)
    REFERENCES genopub.AnnotationProperty (idAnnotationProperty)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
   CONSTRAINT FK_AnnotationPropertyOption_PropertyOption FOREIGN KEY FK_AnnotationPropertyOption_PropertyOption (idPropertyOption)
    REFERENCES genopub.PropertyOption (idPropertyOption)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
)ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;


-- Add new Table AnnotationPropertyValue
DROP TABLE IF EXISTS AnnotationPropertyValue;
CREATE TABLE genopub.AnnotationPropertyValue (
  idAnnotationPropertyValue INT(10) UNSIGNED NOT NULL AUTO_INCREMENT ,
  value VARCHAR(200) NULL,
  idAnnotationProperty INT(10) unsigned  NOT NULL,
  PRIMARY KEY (idAnnotationPropertyValue),
  CONSTRAINT FK_AnnotationPropertyValue_AnnotationProperty FOREIGN KEY FK_AnnotationPropertyValue_AnnotationProperty (idAnnotationProperty)
    REFERENCES genopub.AnnotationProperty (idAnnotationProperty)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
)ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;



/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;