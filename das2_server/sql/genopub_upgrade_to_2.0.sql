-- Add new Table Institute
DROP TABLE IF EXISTS genopub.Institute;
CREATE  TABLE genopub.Institute (
  idInstitute INT(10) UNSIGNED NOT NULL AUTO_INCREMENT ,
  name VARCHAR(200) NOT NULL ,
  description VARCHAR(500) NULL ,
  isActive CHAR(1) NULL DEFAULT 'Y' ,
  PRIMARY KEY (idInstitute) ) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;


    
    
-- Add new Table to link UserGroup to multiple Institutes
DROP TABLE IF EXISTS genopub.InstituteUserGroup;
CREATE TABLE genopub.InstituteUserGroup ( 
    idInstitute	 INT(10) unsigned,
    idUserGroup            INT(10) unsigned,
    PRIMARY KEY (idInstitute, idUserGroup),
CONSTRAINT FK_InstituteUserGroup_Institute FOREIGN KEY FK_InstituteUserGroup_Institute (idInstitute)
    REFERENCES genopub.Institute (idInstitute)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
CONSTRAINT FK_InstituteUserGroup_UserGroup FOREIGN KEY FK_InstituteUserGroup_UserGroup (idUserGroup)
    REFERENCES genopub.UserGroup (idUserGroup)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;

-- Add idInstitute to Annotation
ALTER TABLE genopub.Annotation add column idInstitute INT(10) unsigned;
ALTER TABLE genopub.Annotation add  KEY `FK_Annotation_Institute`  (`idInstitute`);
ALTER TABLE genopub.Annotation add CONSTRAINT FK_Annotation_Institute FOREIGN KEY FK_Annotation_Institute (idInstitute)
    REFERENCES genopub.Institute (idInstitute)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;
    
    
-- Add Collaborator to Annotation
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
)ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;


    
-- Add new Institute visibility level    
INSERT INTO genopub.Visibility (codeVisibility, name) values ('OWNER', 'Owner');
INSERT INTO genopub.Visibility (codeVisibility, name) values ('INST', 'Institute');

-- Convert MEMCOLLAB visibility to MEM
-- update genopub.Annotation set codeVisibility = 'MEM' where codeVisibility = 'MEMCOL';
-- delete from genopub.Visiblity where codeVisibility = 'MEMCOL';



    