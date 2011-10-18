--
-- Add UCSC string name for genome verisons to table `GenomeVersion`
--
ALTER TABLE genopub.GenomeVersion add column ucscName varchar(100) default NULL;

--
-- Add UCSC string name for url to table `User`
--
ALTER TABLE genopub.User add column ucscUrl varchar(250) default 'http://genome.ucsc.edu';