package com.affymetrix.genometryImpl;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.symloader.PSL;
import com.affymetrix.genometryImpl.symloader.SymLoader;
import com.affymetrix.genometryImpl.symloader.SymLoaderFactory;
import com.affymetrix.genometryImpl.util.ServerUtils;

/**
 * OSGi Activator for igb bundle
 */
public class Activator implements BundleActivator {
	private static final String SERVICE_FILTER = "(objectClass=" + SymLoaderFactory.class.getName() + ")";
	protected BundleContext bundleContext;

	@Override
	public void start(BundleContext _bundleContext) throws Exception {
		this.bundleContext = _bundleContext;
		if (bundleContext.getProperty("args") != null) {
			String[] args = bundleContext.getProperty("args").split(", ");
			if (NibbleResiduesParser.FASTA_TO_BNIB.equals(args[0])) {
				String[] runArgs = Arrays.copyOfRange(args, 1, args.length);
				NibbleResiduesParser.main(runArgs);
				System.exit(0);
			}
		}
		try {
			bundleContext.addServiceListener(
				new ServiceListener() {
					@Override
					public void serviceChanged(ServiceEvent event) {
						ServiceReference serviceReference = event.getServiceReference();
						if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.MODIFIED || event.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
							ServerUtils.removeSymLoaderFactory((SymLoaderFactory)bundleContext.getService(serviceReference));
						}
						if (event.getType() == ServiceEvent.REGISTERED || event.getType() == ServiceEvent.MODIFIED) {
							ServerUtils.addSymLoaderFactory((SymLoaderFactory)bundleContext.getService(serviceReference));
						}
					}
				}
			, SERVICE_FILTER);
		}
		catch (InvalidSyntaxException x) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "error loading FloatTransforms 2", x.getMessage());
		}
		registerSymLoaderFactories();
	}

	private interface SymLoaderCreator {
		public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) throws Exception;
	}

	private void registerSymLoaderFactory(final String[] extensions, final Class<?> clazz) {
		registerSymLoaderFactory(extensions,
			new SymLoaderCreator() {
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) throws Exception {
					Constructor<?> con = clazz.getConstructor(URI.class, String.class, AnnotatedSeqGroup.class);
					return (SymLoader)con.newInstance(uri, featureName, group);
				}
			}
		);
	}

	private void registerSymLoaderFactory(final String[] extensions, final SymLoaderCreator symLoaderCreator) {
		SymLoaderFactory factory = new SymLoaderFactory() {
			@Override
			public String[] getExtensions() {
				return extensions;
			}

			@Override
			public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) {
				try {
					SymLoader symLoader = symLoaderCreator.createSymLoader(uri, featureName, group);
					return symLoader;
				}
				catch(Exception x) {
					Logger.getLogger(Activator.class.getName()).log(Level.SEVERE, "constructor failed for extension {0}!!! {1}", new Object[]{extensions[0], x.getMessage()});
				}
				return null;
			}
			
		};
		bundleContext.registerService(
			SymLoaderFactory.class.getName(),
			factory,
			new Properties()
		);
	}
	
	private void registerSymLoaderFactories() {
		// residue loaders
		registerSymLoaderFactory(new String[]{"bnib"}, com.affymetrix.genometryImpl.symloader.BNIB.class);
		registerSymLoaderFactory(new String[]{"fa", "fas", "fasta"}, com.affymetrix.genometryImpl.symloader.Fasta.class);
		registerSymLoaderFactory(new String[]{"2bit"}, com.affymetrix.genometryImpl.symloader.TwoBit.class);
		// symmetry loaders
		registerSymLoaderFactory(new String[]{"bam"}, com.affymetrix.genometryImpl.symloader.BAM.class);
//		registerSymLoaderFactory(new String[]{"bar"}, com.affymetrix.genometryImpl.symloader.Bar.class);
		registerSymLoaderFactory(new String[]{"bed"}, com.affymetrix.genometryImpl.symloader.BED.class);
		registerSymLoaderFactory(new String[]{"gb"}, com.affymetrix.genometryImpl.symloader.Genbank.class);
		registerSymLoaderFactory(new String[]{"gr"}, com.affymetrix.genometryImpl.symloader.Gr.class);
		registerSymLoaderFactory(new String[]{"sgr"}, com.affymetrix.genometryImpl.symloader.Sgr.class);
		// commented out until the USeq class is updated
		registerSymLoaderFactory(new String[]{"useq"}, com.affymetrix.genometryImpl.symloader.USeq.class);
		registerSymLoaderFactory(new String[]{"wig", "bedgraph"}, com.affymetrix.genometryImpl.symloader.Wiggle.class);
		registerSymLoaderFactory(new String[]{"link.psl"},
			new SymLoaderCreator() {
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) throws Exception {
					Constructor<?> con = PSL.class.getConstructor(URI.class, String.class, AnnotatedSeqGroup.class);
					PSL psl = (PSL)con.newInstance(uri, featureName, group);
					psl.setIsLinkPsl(true);
					psl.enableSharedQueryTarget(true);
					return psl;
				}
			}
		);
		registerSymLoaderFactory(new String[]{"psl", "psl3", "pslx"},
			new SymLoaderCreator() {
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) throws Exception {
					Constructor<?> con = PSL.class.getConstructor(URI.class, String.class, AnnotatedSeqGroup.class);
					PSL psl = (PSL)con.newInstance(uri, featureName, group);
					psl.enableSharedQueryTarget(true);
					return psl;
				}
			}
		);
		registerSymLoaderFactory(new String[]{"bgn", "bp1", "bp2", "bps", "brs", "cnt", "cyt"}, com.affymetrix.genometryImpl.symloader.SymLoaderInst.class);
		registerSymLoaderFactory(new String[]{"sin", "egr", "bgr", "bar", "chp"}, com.affymetrix.genometryImpl.symloader.SymLoaderInstNC.class);
		registerSymLoaderFactory(new String[]{"gff3"}, com.affymetrix.genometryImpl.symloader.GFF3.class);
		registerSymLoaderFactory(new String[]{"gff", "gtf"},
			new SymLoaderCreator() {
				public SymLoader createSymLoader(URI uri, String featureName, AnnotatedSeqGroup group) throws Exception {
					Constructor<?> con;
					if(com.affymetrix.genometryImpl.symloader.GFF3.isGFF3(uri)) {
						con = com.affymetrix.genometryImpl.symloader.GFF3.class.getConstructor(URI.class, String.class, AnnotatedSeqGroup.class);
					}
					else {
						con = com.affymetrix.genometryImpl.symloader.SymLoaderInstNC.class.getConstructor(URI.class, String.class, AnnotatedSeqGroup.class);
					}
					return (SymLoader)con.newInstance(uri, featureName, group);
				}
			}
		);
	}

	@Override
	public void stop(BundleContext _bundleContext) throws Exception {}
}
