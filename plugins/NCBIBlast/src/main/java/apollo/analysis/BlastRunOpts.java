package apollo.analysis;

/**
 *
 * @author hiralv
 */
public interface BlastRunOpts {

    public RemoteBlastNCBI.BlastOptions getBlastOptions();

    public RemoteBlastNCBI.BlastType getBlastType();
}
