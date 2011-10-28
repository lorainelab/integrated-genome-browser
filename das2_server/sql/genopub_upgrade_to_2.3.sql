--
-- Add datapath to Annotation
--
ALTER TABLE genopub.Annotation add column dataPath varchar(500) default NULL;

--
-- Add datapath to GenomeVersion
--
ALTER TABLE genopub.GenomeVersion add column dataPath varchar(500) default NULL;
