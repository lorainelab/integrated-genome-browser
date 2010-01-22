Bam files for visualization prototyping and checking.
Bam combined: one alignment file of reads with some mutations (no indels!), and qualities
Bam paired: same alignments as in combined but paired mapping, therefore more, but no qualities.

Screenshots were taken with IGV 1.5 dev.
+ One sees nicely the individual reads, their direction and mutations.
+ Quality of the reads are indicated in case of Mutations: mutations are greyed out
  if qualities are low. see screenshot igv_snapshot_zoomed_in_pop1.png where in the
  3. row the green A is almost not visible anymore (phred quality = 0)

Possible improvements over IGV (or things that are not shown, because the alignment file lacks them):
I see no indication of paired reads besides in the popup (where I wonder if its correct).
Paired reads should be connected somehow
There should be some indication of dangling reads
There should be an indication of inversion



