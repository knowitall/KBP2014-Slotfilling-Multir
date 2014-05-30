package edu.washington.cs.knowitall.kbp2014.multir.slotfiller;

import edu.washington.multirframework.argumentidentification.NERArgumentIdentificationPlusMISC;
import edu.washington.multirframework.featuregeneration.DefaultFeatureGenerator;
import edu.washington.multirframework.argumentidentification.*;

public class MultiModelMultirExtractorVersion1 extends MultiModelMultirExtractor {
	
	public MultiModelMultirExtractorVersion1(){
		super();
		fg = new DefaultFeatureGenerator();
		ai = NERArgumentIdentificationPlusMISC.getInstance();
		sigs.add(FigerAndNERTypeSignatureORGPERSententialInstanceGeneration.getInstance());
		sigs.add(FigerAndNERTypeSignaturePERPERSententialInstanceGeneration.getInstance());
		sigs.add(FigerAndNERTypeSignaturePERLOCSententialInstanceGeneration.getInstance());
		modelFilePaths.add("/projects/WebWare6/Multir/MultirSystem/files/models/Baseline-ORGPER-Multir");
		modelFilePaths.add("/projects/WebWare6/Multir/MultirSystem/files/models/Baseline-PERPER-Multir");
		modelFilePaths.add("/projects/WebWare6/Multir/MultirSystem/files/models/Baseline-PERLOC-Multir");	
	}
}
