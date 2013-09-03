package apollo.analysis;

/**
 *
 * @author hiralv
 */
public interface BlastOptionsI {
	public static RemoteBlastNCBI.BlastType DEFAULT_BLAST_TYPE = RemoteBlastNCBI.BlastType.blastn;
	
	public RemoteBlastNCBI.BlastType getBlastType();
}
