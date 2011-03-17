-- Add new Table PropertyType
DROP TABLE IF EXISTS PropertyType;
CREATE  TABLE genopub.PropertyType (
  codePropertyType VARCHAR(10) NOT NULL ,
  name VARCHAR(200) NOT NULL ,
  isActive CHAR(1) NULL DEFAULT 'Y' ,
PRIMARY KEY (codePropertyType)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;

-- Add new Table Property
DROP TABLE IF EXISTS Property;
CREATE  TABLE genopub.Property (
  idProperty INT(10) UNSIGNED NOT NULL AUTO_INCREMENT ,
  name VARCHAR(200) NOT NULL ,
  isActive CHAR(1) NULL DEFAULT 'Y' ,
  codePropertyType VARCHAR(10) NOT NULL,
  `idUser` int(10) unsigned default NULL,
  PRIMARY KEY (idProperty),
  KEY `FK_Property_User` (`idUser`),
  KEY `FK_Property_PropertyType` (`codePropertyType`),
  CONSTRAINT `FK_Property_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_Property_PropertyType` FOREIGN KEY (`codePropertyType`) REFERENCES `PropertyType` (`codePropertyType`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;

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


-- Insert entries into PropertyType
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('TEXT',        'Text');
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('URL',         'URL');
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('CHECK',       'Checkbox');
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('OPTION',      'Option (Single selection)');
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('MOPTION',     'Option (Multiple selection)');

--
-- Convert Analysis Type to Property
--

-- convert dictionary to Property
insert into Property (name, codePropertyType, isActive) values 
  ('Analysis Type',  'OPTION', 'Y');
SELECT @idProperty:=idProperty from Property where name = 'Analysis Type';
SET @sortOrder=0;
insert into PropertyOption (name, isActive, sortOrder, idProperty)
  values ('', 'Y', 0, @idProperty);
insert into PropertyOption (name, isActive, sortOrder, idProperty) 
  select name, isActive, @sortOrder:=@sortOrder+1, @idProperty
  from AnalysisType;  

-- insert AnnotationProperty for all annotations with analysis types filled in
insert into AnnotationProperty (idAnnotation, name, value, idProperty)
select distinct a.idAnnotation, p.name, o.name, @idProperty
from Annotation a
 join AnalysisType at on a.idAnalysisType = at.idAnalysisType
 join PropertyOption o on o.name = at.name and o.idProperty = @idProperty
 join Property p on p.idProperty = @idProperty;
 
-- insert AnnotationPropertyOption for all annotations with analysis types filled in
insert into AnnotationPropertyOption (idAnnotationProperty, idPropertyOption)
select distinct ap.idAnnotationProperty, o.idPropertyOption
from Annotation a
 join AnnotationProperty ap on ap.idAnnotation = a.idAnnotation and ap.idProperty = @idProperty
 join AnalysisType at on a.idAnalysisType = at.idAnalysisType
 join PropertyOption o on o.name = at.name and o.idProperty = @idProperty
 join Property p on p.idProperty = @idProperty;
 

--
-- Convert Experiment Platform to Property
--

-- convert dictionary to Property
insert into Property (name, codePropertyType, isActive) values 
  ('Experiment Platform',  'OPTION', 'Y');
SELECT @idProperty:=idProperty from Property where name = 'Experiment Platform';
SET @sortOrder=0;
insert into PropertyOption (name, isActive, sortOrder, idProperty)
  values ('', 'Y', 0, @idProperty);
insert into PropertyOption (name, isActive, sortOrder, idProperty) 
  select name, isActive, @sortOrder:=@sortOrder+1, @idProperty
  from ExperimentPlatform;  

-- insert AnnotationProperty for all annotations with experiment platforms filled in
insert into AnnotationProperty (idAnnotation, name, value, idProperty)
select distinct a.idAnnotation, p.name, o.name, @idProperty
from Annotation a
 join ExperimentPlatform at on a.idExperimentPlatform = at.idExperimentPlatform
 join PropertyOption o on o.name = at.name and o.idProperty = @idProperty
 join Property p on p.idProperty = @idProperty;
 
-- insert AnnotationPropertyOption for all annotations with experiment platforms filled in
insert into AnnotationPropertyOption (idAnnotationProperty, idPropertyOption)
select distinct ap.idAnnotationProperty, o.idPropertyOption
from Annotation a
 join AnnotationProperty ap on ap.idAnnotation = a.idAnnotation and ap.idProperty = @idProperty
 join ExperimentPlatform at on a.idExperimentPlatform = at.idExperimentPlatform
 join PropertyOption o on o.name = at.name and o.idProperty = @idProperty
 join Property p on p.idProperty = @idProperty;
 

--
-- Convert Experiment Method to Property
--

-- convert dictionary to Property
insert into Property (name, codePropertyType, isActive) values 
  ('Experiment Method',  'OPTION', 'Y');
SELECT @idProperty:=idProperty from Property where name = 'Experiment Method';
SET @sortOrder=0;
insert into PropertyOption (name, isActive, sortOrder, idProperty)
  values ('', 'Y', 0, @idProperty);
insert into PropertyOption (name, isActive, sortOrder, idProperty) 
  select name, isActive, @sortOrder:=@sortOrder+1, @idProperty
  from ExperimentMethod;  

-- insert AnnotationProperty for all annotations with experiment platforms filled in
insert into AnnotationProperty (idAnnotation, name, value, idProperty)
select distinct a.idAnnotation, p.name, o.name, @idProperty
from Annotation a
 join ExperimentMethod at on a.idExperimentMethod = at.idExperimentMethod
 join PropertyOption o on o.name = at.name and o.idProperty = @idProperty
 join Property p on p.idProperty = @idProperty;
 
-- insert AnnotationPropertyOption for all annotations with experiment platforms filled in
insert into AnnotationPropertyOption (idAnnotationProperty, idPropertyOption)
select distinct ap.idAnnotationProperty, o.idPropertyOption
from Annotation a
 join AnnotationProperty ap on ap.idAnnotation = a.idAnnotation and ap.idProperty = @idProperty
 join ExperimentMethod at on a.idExperimentMethod = at.idExperimentMethod
 join PropertyOption o on o.name = at.name and o.idProperty = @idProperty
 join Property p on p.idProperty = @idProperty;
 
 
 -- Now drop idAnalysisType, idExperimentPlatform, idExperimentMethod from Annotation
 alter table Annotation drop foreign key FK_Annotation_AnalysisType;
 alter table Annotation drop column idAnalysisType;
 alter table Annotation drop foreign key FK_Annotation_ExperimentPlatform;
 alter table Annotation drop column idExperimentPlatform;
 alter table Annotation drop foreign key FK_Annotation_ExperimentMethod;
 alter table Annotation drop column idExperimentMethod;
 
 -- Get rid of AnalysisType, ExperimentPlatform, ExperimentMethod
 drop table AnalysisType;
 drop table ExperimentPlatform;
 drop table ExperimentMethod; 
 

