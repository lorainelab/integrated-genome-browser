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
) ENGINE=InnoDB AUTO_INCREMENT=6300 DEFAULT CHARSET=latin1;
 

INSERT INTO `AnnotationGrouping` (`idAnnotationGrouping`,`idGenomeVersion`, `name`,`description`) VALUES 
(100,100,'anoCar1','anoCar1')
(200,200,'anoGam1','anoGam1')
(200,200,'apiMel2','apiMel2')
(400,400,'A_thaliana_Jan_2004','A_thaliana_Jan_2004')
(401,401,'A_thaliana_TAIR7','A_thaliana_TAIR7')
(402,402,'A_thalina_TAIR8','A_thalina_TAIR8')
(403,403,'A_thaliana_TAIR9','A_thaliana_TAIR9')
(500,500,'B_taurus_Aug_2006','B_taurus_Aug_2006')
(501,501,'B_taurus_Oct_2007','B_taurus_Oct_2007')
(502,502,'bosTau2','bosTau2')
(503,503,'bosTau3','bosTau3')
(504,504,'bosTau4','bosTau4')
(600,600,'braFlo1','braFlo1')
(700,700,'calJac1','calJac1')
(800,800,'cavPor3','cavPor3')
(900,900,'ci1','ci1')
(900,900,'ci2','ci2')
(1000,1000,'caePb1','caePb1')
(1001,1001,'caePb2','caePb2')
(1100,1100,'cb1','cb1')
(1101,1101,'cb3','cb3')
(1102,1102,'briggsae','briggsae')
(1103,1103,'briggsae_cb25','briggsae_cb25')
(1200,1200,'C_elegans_May_2003','C_elegans_May_2003')
(1201,1201,'C_elegans_Mar_2004','C_elegans_Mar_2004')
(1202,1202,'C_elegans_Jan_2007','C_elegans_Jan_2007')
(1203,1203,'C_elegans_May_2007','C_elegans_May_2007')
(1204,1204,'C_elegans_May_2008','C_elegans_May_2008')
(1205,1205,'ce2','ce2')
(1206,1206,'ce4','ce4')
(1207,1207,'ce6','ce6')
(1208,1208,'elegans','elegans')
(1209,1209,'elegans_ws110','elegans_ws110')
(1300,1300,'caeJap1','caeJap1')
(1400,1400,'caeRem2','caeRem2')
(1401,1401,'caeRem3','caeRem3')
(1402,1402,'remanei','remanei')
(1500,1500,'C_familiaris_May_2005','C_familiaris_May_2005')
(1501,1501,'canFam1','canFam1')
(1502,1502,'canFam2','canFam2')
(1600,1600,'droAna1','droAna1')
(1601,1601,'droAna2','droAna2')
(1700,1700,'droEre1','droEre1')
(1800,1800,'droGri1','droGri1')
(1900,1900,'D_melanogaster_Apr_2004','D_melanogaster_Apr_2004')
(1901,1901,'D_melanogaster_Apr_2006','D_melanogaster_Apr_2006')
(1902,1902,'D_melanogaster_Jan_2003','D_melanogaster_Jan_2003')
(1903,1903,'dm1','dm1')
(1904,1904,'dm2','dm2')
(1905,1905,'dm3','dm3')
(1906,1906,'droMoj1','droMoj1')
(1907,1907,'droMoj2','droMoj2')
(2100,2100,'droPer1','droPer1')
(2200,2200,'dp2','dp2')
(2201,2201,'dp3','dp3')
(2300,2300,'droSec1','droSec1')
(2400,2400,'droSim1','droSim1')
(2500,2500,'droVir1','droVir1')
(2501,2501,'droVir2','droVir2')
(2600,2600,'droYak1','droYak1')
(2601,2601,'droYak2','droYak2')
(2700,2700,'D_rerio_Mar_2006','D_rerio_Mar_2006')
(2701,2701,'D_rerio_Jul_2007','D_rerio_Jul_2007')
(2702,2702,'danRer3','danRer3')
(2703,2703,'danRer4','danRer4')
(2704,2704,'danRer5','danRer5')
(2705,2705,'equCab1','equCab1')
(2800,2800,'equCab2','equCab2')
(2900,2900,'E_coli_oct_2007','E_coli_oct_2007')
(2901,2901,'e-coli_oct-2007','e-coli_oct-2007')
(3000,3000,'felCat3','felCat3')
(3100,3100,'G_gallus_May_2006','G_gallus_May_2006')
(3101,3101,'galGal2','galGal2')
(3102,3102,'galGal3','galGal3')
(3202,3202,'','')
(3301,3301,'G_max_Dec_2008','G_max_Dec_2008')
(2400,2400,'H_sapiens_Jun_2002','H_sapiens_Jun_2002')
(2401,2401,'H_sapiens_Nov_2002','H_sapiens_Nov_2002')
(3402,3402,'H_sapiens_Apr_2003','H_sapiens_Apr_2003')
(2403,2403,'H_sapiens_Jul_2003','H_sapiens_Jul_2003')
(3404,3404,'H_sapiens_May_2004','H_sapiens_May_2004')
(3405,3405,'H_sapiens_Mar_2006','H_sapiens_Mar_2006')
(3406,3406,'hg16','hg16')
(3407,3407,'hg17','hg17')
(3408,3408,'hg18','hg18')
(3409,3409,'hg19','hg19')
(2500,2500,'M_mulatta_Jan_2006','M_mulatta_Jan_2006')
(3501,3501,'rheMac2','rheMac2')
(3600,3600,'M_truncatula_Aug_2007','M_truncatula_Aug_2007')
(3700,3700,'monDom1','monDom1')
(3701,3701,'monDom4','monDom4')
(3702,3702,'monDom5','monDom5')
(3703,3703,'M_musculus_Feb_2002','M_musculus_Feb_2002')
(3604,3604,'M_musculus_Feb_2003','M_musculus_Feb_2003')
(3800,3800,'M_musculus_Oct_2003','M_musculus_Oct_2003')
(3801,3801,'M_musculus_Aug_2005','M_musculus_Aug_2005')
(3802,3802,'M_musculus_Mar_2006','M_musculus_Mar_2006')
(3803,3803,'M_musculus_Feb_2002','M_musculus_Feb_2002')
(3804,3804,'M_musculus_Jul_2007','M_musculus_Jul_2007')
(3805,3805,'mm7','mm7')
(3806,3806,'mm8','mm8')
(3807,3807,'mm9','mm9')
(3808,3808,'mouse_feb-2006_mm8','mouse_feb-2006_mm8')
(3809,3809,'Mm:NCBIv37','Mm:NCBIv37')
(3900,3900,'M_abscessus_Mar_2008','M_abscessus_Mar_2008')
(4000,4000,'M_smegmatis_mc2-155_march_2008','M_smegmatis_mc2-155_march_2008')
(4100,4100,'M_tuberculosis_h37Rv_sep_2008','M_tuberculosis_h37Rv_sep_2008')
(4101,4101,'mtuberculosis-h37Rv_sep2008','mtuberculosis-h37Rv_sep2008')
(4200,4200,'ornAna1','ornAna1')
(4300,4300,'O_sativa_Jan_2007','O_sativa_Jan_2007')
(4301,4301,'O_sativa_Jan_2009','O_sativa_Jan_2009')
(4302,4302,'O_sativa_Jun_2009','O_sativa_Jun_2009')
(4400,4400,'oryLat2','oryLat2')
(4500,4500,'','')
(4600,4600,'panTro1','panTro1')
(4601,4601,'panTro2','panTro2')
(4701,4701,'petMar1','petMar1')
(4800,4800,'P_falciparum_Jul_2007','P_falciparum_Jul_2007')
(4801,4801,'Pfalciparum_3D7_plasmoDB-5.4','Pfalciparum_3D7_plasmoDB-5.4')
(4802,4802,'Pfalciparum_3D7_plasmoDB-5.5','Pfalciparum_3D7_plasmoDB-5.5')
(4900,4900,'ponAbe2','ponAbe2')
(5000,5000,'P_trichocarpa_Jun_2004','P_trichocarpa_Jun_2004')
(5100,5100,'priPac1','priPac1')
(5200,5200,'R_norvegicus_Jan_2003','R_norvegicus_Jan_2003')
(5201,5201,'R_norvegicus_Jun_2003','R_norvegicus_Jun_2003')
(5202,5202,'R_norvegicus_Nov_2004','R_norvegicus_Nov_2004')
(5203,5203,'rn3','rn3')
(5204,5204,'rn4','rn4')
(5300,5300,'S_cerevisiae_Oct_2003','S_cerevisiae_Oct_2003')
(5301,5301,'sacCer1','sacCer1')
(5302,5302,'yeast_feb2006_37_1d','yeast_feb2006_37_1d')
(5303,5303,'yeast_may2008','yeast_may2008')
(5304,5304,'S_cerevisiae_feb_2006','S_cerevisiae_feb_2006')
(5305,5305,'S_cerevisiae_Jul_2007','S_cerevisiae_Jul_2007')
(5306,5306,'S_cerevisiae_Apr_2008','S_cerevisiae_Apr_2008')
(5207,5207,'S_cerevisiae_may_2008','S_cerevisiae_may_2008')
(5308,5308,'yeast_2005Jan','yeast_2005Jan')
(5309,5309,'yeast_Ver1','yeast_Ver1')
(5400,5400,'S_pombe_Sep_2004','S_pombe_Sep_2004')
(5401,5401,'S_pombe_Apr_2007','S_pombe_Apr_2007')
(5402,5402,'S_pombe_sep_2007','S_pombe_sep_2007')
(5403,5403,'pombe_sep-2007','pombe_sep-2007')
(5404,5404,'','')
(5600,5600,'','')
(5700,5700,'strPur1','strPur1')
(5701,5701,'strPur2','strPur2')
(5800,5800,'','')
(5900,5900,'fr2','fr2')
(6000,6000,'T_nigroviridis_feb_2004','T_nigroviridis_feb_2004')
(6100,6100,'V_vinifera_Apr_2007','V_vinifera_Apr_2007')
(6200,6200,'xenTro2','xenTro2')
(6201,6201,'X_tropicalis_aug_2005;','X_tropicalis_aug_2005');


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
) ENGINE=InnoDB AUTO_INCREMENT=6300 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `GenomeVersion`
--

/*!40000 ALTER TABLE `GenomeVersion` DISABLE KEYS */;
INSERT INTO `GenomeVersion` (`idGenomeVersion`, `idOrganism`, `name`, `buildDate`, `coordURI`,`coordVersion`,`coordSource`,`coordTestRange`,`coordAuthority`) VALUES
(100,100,'anoCar1',NULL, NULL, NULL, NULL, NULL, NULL),
(200,200,'anoGam1',NULL, NULL, NULL, NULL, NULL, NULL),
(200,300,'apiMel2',NULL, NULL, NULL, NULL, NULL, NULL),
(400,400,'A_thaliana_Jan_2004','2004/1/1 0:00', NULL, NULL, NULL, NULL, NULL),
(401,400,'A_thaliana_TAIR7',NULL, NULL, NULL, NULL, NULL, NULL),
(402,400,'A_thalina_TAIR8',NULL, NULL, NULL, NULL, NULL, NULL),
(403,400,'A_thaliana_TAIR9',NULL, NULL, NULL, NULL, NULL, NULL),
(500,500,'B_taurus_Aug_2006','2006/8/1 0:00', NULL, NULL, NULL, NULL, NULL),
(501,500,'B_taurus_Oct_2007','2007/10/1 0:00', NULL, NULL, NULL, NULL, NULL),
(502,500,'bosTau2',NULL, NULL, NULL, NULL, NULL, NULL),
(503,500,'bosTau3',NULL, NULL, NULL, NULL, NULL, NULL),
(504,500,'bosTau4',NULL, NULL, NULL, NULL, NULL, NULL),
(600,600,'braFlo1',NULL, NULL, NULL, NULL, NULL, NULL),
(700,700,'calJac1',NULL, NULL, NULL, NULL, NULL, NULL),
(800,800,'cavPor3',NULL, NULL, NULL, NULL, NULL, NULL),
(900,900,'ci1',NULL, NULL, NULL, NULL, NULL, NULL),
(900,900,'ci2',NULL, NULL, NULL, NULL, NULL, NULL),
(1000,1000,'caePb1',NULL, NULL, NULL, NULL, NULL, NULL),
(1001,1000,'caePb2',NULL, NULL, NULL, NULL, NULL, NULL),
(1100,1100,'cb1',NULL, NULL, NULL, NULL, NULL, NULL),
(1101,1100,'cb3',NULL, NULL, NULL, NULL, NULL, NULL),
(1102,1100,'briggsae',NULL, NULL, NULL, NULL, NULL, NULL),
(1103,1100,'briggsae_cb25',NULL, NULL, NULL, NULL, NULL, NULL),
(1200,1200,'C_elegans_May_2003','2003/5/1 0:00', NULL, NULL, NULL, NULL, NULL),
(1201,1200,'C_elegans_Mar_2004','2004/3/1 0:00', NULL, NULL, NULL, NULL, NULL),
(1202,1200,'C_elegans_Jan_2007','2007/1/1 0:00', 'http://www.wormbase.org/genome/C_elegans/WS180/','180','Chromosome','','WS'),
(1203,1200,'C_elegans_May_2007','2007/5/1 0:00', NULL, NULL, NULL, NULL, NULL),
(1204,1200,'C_elegans_May_2008','2008/5/1 0:00', NULL, NULL, NULL, NULL, NULL),
(1205,1200,'ce2',NULL, NULL, NULL, NULL, NULL, NULL),
(1206,1200,'ce4',NULL, NULL, NULL, NULL, NULL, NULL),
(1207,1200,'ce6',NULL, NULL, NULL, NULL, NULL, NULL),
(1208,1200,'elegans',NULL, NULL, NULL, NULL, NULL, NULL),
(1209,1200,'elegans_ws110',NULL, NULL, NULL, NULL, NULL, NULL),
(1300,1300,'caeJap1',NULL, NULL, NULL, NULL, NULL, NULL),
(1400,1400,'caeRem2',NULL, NULL, NULL, NULL, NULL, NULL),
(1401,1400,'caeRem3',NULL, NULL, NULL, NULL, NULL, NULL),
(1402,1400,'remanei',NULL, NULL, NULL, NULL, NULL, NULL),
(1500,1500,'C_familiaris_May_2005','2005/5/1 0:00', NULL, NULL, NULL, NULL, NULL),
(1501,1500,'canFam1',NULL, NULL, NULL, NULL, NULL, NULL);
(1502,1500,'canFam2',NULL, NULL, NULL, NULL, NULL, NULL),
(1600,1600,'droAna1',NULL, NULL, NULL, NULL, NULL, NULL),
(1601,1600,'droAna2',NULL, NULL, NULL, NULL, NULL, NULL),
(1700,1700,'droEre1',NULL, NULL, NULL, NULL, NULL, NULL),
(1800,1800,'droGri1',NULL, NULL, NULL, NULL, NULL, NULL),
(1900,1900,'D_melanogaster_Apr_2004','2004/4/1 0:00', NULL, NULL, NULL, NULL, NULL),
(1901,1900,'D_melanogaster_Apr_2006','2006/4/1 0:00', NULL, NULL, NULL, NULL, NULL),
(1902,1900,'D_melanogaster_Jan_2003','2003/1/1 0:00', NULL, NULL, NULL, NULL, NULL),
(1903,1900,'dm1',NULL, NULL, NULL, NULL, NULL, NULL),
(1904,1900,'dm2',NULL, NULL, NULL, NULL, NULL, NULL),
(1905,1900,'dm3',NULL, NULL, NULL, NULL, NULL, NULL),
(1906,2000,'droMoj1',NULL, NULL, NULL, NULL, NULL, NULL),
(1907,2000,'droMoj2',NULL, NULL, NULL, NULL, NULL, NULL),
(2100,2100,'droPer1',NULL, NULL, NULL, NULL, NULL, NULL),
(2200,2200,'dp2',NULL, NULL, NULL, NULL, NULL, NULL),
(2201,2200,'dp3',NULL, NULL, NULL, NULL, NULL, NULL),
(2300,2300,'droSec1',NULL, NULL, NULL, NULL, NULL, NULL),
(2400,2400,'droSim1',NULL, NULL, NULL, NULL, NULL, NULL),
(2500,2500,'droVir1',NULL, NULL, NULL, NULL, NULL, NULL),
(2501,2500,'droVir2',NULL, NULL, NULL, NULL, NULL, NULL),
(2600,2600,'droYak1',NULL, NULL, NULL, NULL, NULL, NULL),
(2601,2600,'droYak2',NULL, NULL, NULL, NULL, NULL, NULL),
(2700,2700,'D_rerio_Mar_2006','2006/3/1 0:00', NULL, NULL, NULL, NULL, NULL),
(2701,2700,'D_rerio_Jul_2007','2007/7/1 0:00', 'http://zfin.org/genome/D_rerio/Zv7/','Zv7','Chromosome',NULL,'ZFISH_7'),
(2702,2700,'danRer3',NULL, NULL, NULL, NULL, NULL, NULL),
(2703,2700,'danRer4',NULL, NULL, NULL, NULL, NULL, NULL),
(2704,2700,'danRer5',NULL, NULL, NULL, NULL, NULL, NULL),
(2705,2800,'equCab1',NULL, NULL, NULL, NULL, NULL, NULL),
(2800,2800,'equCab2',NULL, NULL, NULL, NULL, NULL, NULL),
(2900,2900,'E_coli_oct_2007','2007/10/1 0:00', NULL, NULL, NULL, NULL, NULL),
(2901,2900,'e-coli_oct-2007','2007/10/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3000,3000,'felCat3',NULL, NULL, NULL, NULL, NULL, NULL),
(3100,3100,'G_gallus_May_2006','2006/5/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3101,3100,'galGal2',NULL, NULL, NULL, NULL, NULL, NULL),
(3102,3100,'galGal3',NULL, NULL, NULL, NULL, NULL, NULL),
(3202,3200,'',NULL, NULL, NULL, NULL, NULL, NULL),
(3301,3300,'G_max_Dec_2008','2008/12/1 0:00', NULL, NULL, NULL, NULL, NULL),
(2400,3400,'H_sapiens_Jun_2002','2002/6/1 0:00', NULL, NULL, NULL, NULL, NULL),
(2401,3400,'H_sapiens_Nov_2002','2002/11/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3402,3400,'H_sapiens_Apr_2003','2003/4/1 0:00', NULL, NULL, NULL, NULL, NULL),
(2403,3400,'H_sapiens_Jul_2003','2003/7/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3404,3400,'H_sapiens_May_2004','2004/5/1 0:00', 'http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B35.1/','35','Chromosome',NULL,'NCBI'),
(3405,3400,'H_sapiens_Mar_2006','2006/3/1 0:00', 'http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B36.1/','36','Chromosome','','NCBI'),
(3406,3400,'hg16',NULL, NULL, NULL, NULL, NULL, NULL),
(3407,3400,'hg17',NULL, NULL, NULL, NULL, NULL, NULL),
(3408,3400,'hg18',NULL, NULL, NULL, NULL, NULL, NULL),
(3409,3400,'hg19',NULL, NULL, NULL, NULL, NULL, NULL),
(2500,3500,'M_mulatta_Jan_2006','2006/1/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3501,3500,'rheMac2',NULL, NULL, NULL, NULL, NULL, NULL),
(3600,3600,'M_truncatula_Aug_2007','2007/8/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3700,3700,'monDom1',NULL, NULL, NULL, NULL, NULL, NULL),
(3701,3700,'monDom4',NULL, NULL, NULL, NULL, NULL, NULL),
(3702,3700,'monDom5',NULL, NULL, NULL, NULL, NULL, NULL),
(3703,3800,'M_musculus_Feb_2002','2002/2/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3604,3800,'M_musculus_Feb_2003','2003/2/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3800,3800,'M_musculus_Oct_2003','2003/10/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3801,3800,'M_musculus_Aug_2005','2005/8/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3802,3800,'M_musculus_Mar_2006','2006/3/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3803,3800,'M_musculus_Feb_2002','2002/2/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3804,3800,'M_musculus_Jul_2007','2007/7/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3805,3800,'mm7',NULL, NULL, NULL, NULL, NULL, NULL),
(3806,3800,'mm8',NULL, NULL, NULL, NULL, NULL, NULL),
(3807,3800,'mm9',NULL, NULL, NULL, NULL, NULL, NULL),
(3808,3800,'mouse_feb-2006_mm8','2006/2/1 0:00', NULL, NULL, NULL, NULL, NULL),
(3809,3800,'Mm:NCBIv37',NULL, NULL, NULL, NULL, NULL, NULL),
(3900,3900,'M_abscessus_Mar_2008','2008/3/1 0:00', NULL, NULL, NULL, NULL, NULL),
(4000,4000,'M_smegmatis_mc2-155_march_2008','2008/3/1 0:00', NULL, NULL, NULL, NULL, NULL),
(4100,4100,'M_tuberculosis_h37Rv_sep_2008','2008/9/1 0:00', NULL, NULL, NULL, NULL, NULL),
(4101,4100,'mtuberculosis-h37Rv_sep2008','2008/9/1 0:00', NULL, NULL, NULL, NULL, NULL),
(4200,4200,'ornAna1',NULL, NULL, NULL, NULL, NULL, NULL),
(4300,4300,'O_sativa_Jan_2007','2007/1/1 0:00', NULL, NULL, NULL, NULL, NULL),
(4301,4300,'O_sativa_Jan_2009','2009/1/1 0:00', NULL, NULL, NULL, NULL, NULL),
(4302,4300,'O_sativa_Jun_2009','2009/6/1 0:00', NULL, NULL, NULL, NULL, NULL),
(4400,4400,'oryLat2',NULL, NULL, NULL, NULL, NULL, NULL),
(4500,4500,'',NULL, NULL, NULL, NULL, NULL, NULL),
(4600,4600,'panTro1',NULL, NULL, NULL, NULL, NULL, NULL),
(4601,4600,'panTro2',NULL, NULL, NULL, NULL, NULL, NULL),
(4701,4700,'petMar1',NULL, NULL, NULL, NULL, NULL, NULL),
(4800,4800,'P_falciparum_Jul_2007','2007/7/1 0:00', NULL, NULL, NULL, NULL, NULL),
(4801,4800,'Pfalciparum_3D7_plasmoDB-5.4',NULL, NULL, NULL, NULL, NULL, NULL),
(4802,4800,'Pfalciparum_3D7_plasmoDB-5.5',NULL, NULL, NULL, NULL, NULL, NULL),
(4900,4900,'ponAbe2',NULL, NULL, NULL, NULL, NULL, NULL),
(5000,5000,'P_trichocarpa_Jun_2004','2004/6/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5100,5100,'priPac1',NULL, NULL, NULL, NULL, NULL, NULL),
(5200,5200,'R_norvegicus_Jan_2003','2003/1/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5201,5200,'R_norvegicus_Jun_2003','2003/6/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5202,5200,'R_norvegicus_Nov_2004','2004/11/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5203,5200,'rn3',NULL, NULL, NULL, NULL, NULL, NULL),
(5204,5200,'rn4',NULL, NULL, NULL, NULL, NULL, NULL),
(5300,5300,'S_cerevisiae_Oct_2003','2003/10/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5301,5300,'sacCer1',NULL, NULL, NULL, NULL, NULL, NULL),
(5302,5300,'yeast_feb2006_37_1d','2006/2/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5303,5300,'yeast_may2008','2008/5/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5304,5300,'S_cerevisiae_feb_2006','2006/2/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5305,5300,'S_cerevisiae_Jul_2007','2007/7/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5306,5300,'S_cerevisiae_Apr_2008','2008/4/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5207,5300,'S_cerevisiae_may_2008','2008/5/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5308,5300,'yeast_2005Jan','2005/1/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5309,5300,'yeast_Ver1',NULL, NULL, NULL, NULL, NULL, NULL),
(5400,5400,'S_pombe_Sep_2004','2004/9/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5401,5400,'S_pombe_Apr_2007','2007/4/1 0:00', 'http://www.sanger.ac.uk/Projects/S_pombe/Apr_2007','Apr_2007','Chromosome','','Sanger'),
(5402,5400,'S_pombe_sep_2007','2007/9/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5403,5400,'pombe_sep-2007','2007/9/1 0:00', NULL, NULL, NULL, NULL, NULL),
(5404,5500,'',NULL, NULL, NULL, NULL, NULL, NULL),
(5500,5500,'S_glossinidius_Jan_2006','2006/01/01 0:0','ftp://ftp.ncbi.nih.gov/genomes/Bacteria/Sodalis_glossinidius_morsitans/Jan_2006','Jan_2006','Chromosome',NULL,'NCBI'),
(5600,5600,'',NULL, NULL, NULL, NULL, NULL, NULL),
(5700,5700,'strPur1',NULL, NULL, NULL, NULL, NULL, NULL),
(5701,5700,'strPur2',NULL, NULL, NULL, NULL, NULL, NULL),
(5800,5800,'',NULL, NULL, NULL, NULL, NULL, NULL),
(5900,5900,'fr2',NULL, NULL, NULL, NULL, NULL, NULL),
(6000,6000,'T_nigroviridis_feb_2004','2004/2/1 0:00', NULL, NULL, NULL, NULL, NULL),
(6100,6100,'V_vinifera_Apr_2007','2007/4/1 0:00', NULL, NULL, NULL, NULL, NULL),
(6200,6200,'xenTro2',NULL, NULL, NULL, NULL, NULL, NULL),
(6201,6200,'X_tropicalis_aug_2005','2005/8/1 0:00', NULL, NULL, NULL, NULL, NULL);




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
) ENGINE=InnoDB AUTO_INCREMENT=6300 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Organism`
--

/*!40000 ALTER TABLE `Organism` DISABLE KEYS */;
INSERT INTO Organism (idOrganism, binomialName, commonName, name, NCBITaxID ) values
(100, 'Anolis carolinensis',	'Lizard',	'A_carolinensis', NULL),
(200, 'Anopheles gambiae',	'A.gambiae',	'A_gambiae', NULL),
(300, 'Apis mellifera',	'A_mellifera',	'apiMel1', NULL),
(400, 'Arabidopsis thaliana',	'A. thaliana',	'A_thaliana', NULL),
(500, 'Bos taurus',	'Cow',	'B_taurus', NULL),
(600, 'Branchiostoma floridae',	'Lancet',	'B_floridae', NULL),
(700, 'Callithrix jacchus',	'Marmoset',	'C_jacchus', NULL),
(800, 'Cavia porcellus',	'Guinea Pig',	'C_porcellus', NULL),
(900, 'Ciona intestinalis',	'C. intestinalis',	'C_intestinalis', NULL),
(1000, 'Caenorhabditis brenneri',	'C. brenneri',	'C_brenneri', NULL),
(1100, 'Caenorhabditis briggsae',	'C. briggsae',	'C_briggsae', NULL),
(1200, 'Caenorhabditis elegans',	'C. elegans',	'C_elegans', '6239'),
(1300, 'Caenorhabditis japonica',	'C. japonica',	'C_japonica', NULL),
(1400, 'Caenorhabditis remanei',	'C. remanei',	'C_remanei', NULL),
(1500, 'Canis lupus familiaris',	'Dog',	'C_familiaris', NULL),
(1600, 'Drosophila ananassae',	'D. ananassae',	'D_ananassae', NULL),
(1700, 'Drosophila erecta',	'D. erecta',	'D_erecta', NULL),
(1800, 'Drosophila grimshawi',	'D. grimshawi',	'D_grimshawi', NULL),
(1900, 'Drosophila melanogaster',	'D. melanogaster',	'D_melanogaster', NULL),
(2000, 'Drosophila mojavensis',	'D. mojavensis',	'D_mojavensis', NULL),
(2100, 'Drosophila persimilis',	'D. persimilis',	'D_persimilis', NULL),
(2200, 'Drosophila pseudoobscura',	'D. pseudoobscura',	'D_pseudoobscura', NULL),
(2300, 'Drosophila sechellia',	'D. sechellia',	'D_sechellia', NULL),
(2400, 'Drosophila simulans',	'D. simulans',	'D_simulans', NULL),
(2500, 'Drosophila virilis',	'D. virilis',	'D_virilis', NULL),
(2600, 'Drosophila yakuba',	'D. yakuba',	'D_yakuba', NULL),
(2700, 'Danio rerio',	'Zebrafish',	'D_rerio', '7955'),
(2800, 'Equus caballus',	'Horse',	'H_caballus', NULL),
(2900, 'Escherichia coli',	'E. coli',	'E_coli', NULL),
(3000, 'Felis catus',	'Cat',	'F_catus', NULL),
(3100, 'Gallus gallus',	'Chicken',	'G_gallus', NULL),
(3200, 'Gasterosteus aculeatus',	'Stickleback G_aculeatus',	'gasAcu1', NULL),
(3300, 'Glycine max',	'Soybean',	'G_max', NULL),
(3400, 'Homo sapiens',	'Human',	'H_sapiens', '9606'),
(3500, 'Macaca mulatta',	'Rhesus',	'M_mulatta', NULL),
(3600, 'Medicago truncatula',	'Barrel Medic',	'M_truncatula', NULL),
(3700, 'Monodelphis domestica',	'Opossum',	'M_domestica', NULL),
(3800, 'Mus musculus',	'Mouse',	'M_musculus', '10090'),
(3900, 'Mycobacterium abscessus',	'M. abcessus',	'M_abscessus', NULL),
(4000, 'Mycobacterium smegmatis',	'M. smegmatis',	'M_smegmatis', NULL),
(4100, 'Mycobacterium tuberculosis',	'M. tuberculosis',	'M_tuberculosis', NULL),
(4200, 'Ornithorhynchus anatinus',	'Platypus',	'O_anatinus', NULL),
(4300, 'Oryza sativa',	'Rice',	'O_sativa', NULL),
(4400, 'Oryzias latipes',	'Medaka',	'O_latipes', NULL),
(4500, 'Ostreococcus lucimarinus', 'O. lucimarinus',	'O_lucimarinus_Apr_2007', NULL),
(4600, 'Pan troglodytes',	'Chimp',	'P_troglodytes', NULL),
(4700, 'Petromyzon marinus',	'Lamprey',	'P_marinus', NULL),
(4800, 'Plasmodium falciparum',	'P. falciparum',	'P_falciparum', NULL),
(4900, 'Pongo pygmaeus abelii',	'Orangutan',	'P_abelii', NULL),
(5000, 'Populus trichocarpa',	'Black Cottonwood',	'P_trichocarpa', NULL),
(5100, 'Pristionchus pacificus',	'P. pacificus',	'P_pacificus', NULL),
(5200, 'Rattus norvegicus',	'Rat',	'R_norvegicus', NULL),
(5300, 'Saccharomyces cerevisiae',	'Yeast',	'S_cerevisiae', NULL),
(5400, 'Schizosaccharomyces pombe',	'Fission Yeast',	'S_pombe', '4896'),
(5500, 'Sodalis glossinidius',	'S. glossinidius', 'S_glossinidius', '343509'),
(5600, 'Sorghum bicolor',	'Sorghum',	'S_bicolor_Mar_2008', NULL),   
(5700, 'Strongylocentrotus purpuratus',	'Purple Sea Urchin',	'S_purpuratus', NULL),
(5800, 'Taeniopygia guttata',	'Zebra Finch',	'taeGut1', NULL),
(5900, 'Takifugu rubripes',	'Fugu',	'fr1', NULL),
(6000, 'Tetraodon nigroviridis',	'Tetraodon',	'tetNig1', NULL),
(6100, 'Vitis vinifera',	'Common Grape Vine',	'V_vinifera', NULL),
(6200, 'Xenopus tropicalis',	'Pipid Frog',	'xenTro1', NULL);
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
 (55,15072419,'chrI',6,1),
 (56,15279316,'chrII',6,2),
 (57,13783681,'chrIII',6,3),
 (58,17493784,'chrIV',6,4),
 (59,20919398,'chrV',6,5),
 (60,13794,'chrM',6,6),
 (61,17718852,'chrX',6,7),
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