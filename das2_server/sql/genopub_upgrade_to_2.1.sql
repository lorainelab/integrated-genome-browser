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
  PRIMARY KEY (idAnnotation, idPropertyOption),
  CONSTRAINT FK_AnnotationPropertyOption_AnnotationProperty FOREIGN KEY FK_AnnotationPropertyOption_AnnotationProperty (idAnnotationProperty)
    REFERENCES genopub.AnnotationProperty (idAnnotationProperty)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
   CONSTRAINT FK_AnnotationPropertyOption_PropertyOption FOREIGN KEY FK_AnnotationPropertyOption_PropertyOption (idPropertyOption)
    REFERENCES genopub.PropertyOption (idPropertyOption)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
)ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;


-- Insert entries into PropertyType
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('TEXT',        'Text');
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('URL',         'URL');
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('CHECK',       'Checkbox');
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('OPTION',      'Option (Single selection)');
INSERT INTO genopub.PropertyType (codePropertyType, name) values ('MOPTION',     'Option (Multiple selection)');

