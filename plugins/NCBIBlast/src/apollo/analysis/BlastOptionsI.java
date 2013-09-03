package apollo.analysis;

/**
 *
 * @author hiralv
 */
public interface BlastOptionsI {
	public static RemoteBlastNCBI.BlastType DEFAULT_BLAST_TYPE = RemoteBlastNCBI.BlastType.blastn;
	public static final String PREF_BLAST_TYPE = "Blast type";
	
	public RemoteBlastNCBI.BlastType getBlastType();
}
