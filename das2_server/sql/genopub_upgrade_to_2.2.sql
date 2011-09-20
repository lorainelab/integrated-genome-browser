--
-- Add UCSC string name for genome verisons to table `GenomeVersion`
--
ALTER TABLE genopub.GenomeVersion add column ucscName varchar(50) default NULL;