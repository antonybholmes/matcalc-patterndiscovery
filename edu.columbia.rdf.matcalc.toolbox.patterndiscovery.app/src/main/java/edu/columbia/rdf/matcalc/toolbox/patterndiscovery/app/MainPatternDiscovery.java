package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.app;



import java.awt.FontFormatException;
import java.io.IOException;

import javax.swing.UnsupportedLookAndFeelException;
import javax.xml.parsers.ParserConfigurationException;

import org.jebtk.core.AppService;
import org.jebtk.modern.ColorTheme;
import org.jebtk.modern.theme.ThemeService;

import edu.columbia.rdf.matcalc.MainMatCalc;
import edu.columbia.rdf.matcalc.ModuleLoader;

import org.xml.sax.SAXException;

import edu.columbia.rdf.matcalc.bio.BioModuleLoader;
import edu.columbia.rdf.matcalc.toolbox.patterndiscovery.PatternDiscoveryModule;
import edu.columbia.rdf.matcalc.toolbox.patterndiscovery.classifier.ClassifierModule;




public class MainPatternDiscovery {
	//private static final Logger LOG = 
	//		LoggerFactory.getLogger(MainSeqLogo.class);
	
	public static final void main(String[] args) throws FontFormatException, IOException, SAXException, ParserConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		AppService.getInstance().setAppInfo("pattern_discovery");
		
		ThemeService.getInstance().setTheme(ColorTheme.ORANGE);
		
		ModuleLoader ml = new BioModuleLoader();
		
		ml.addModule(PatternDiscoveryModule.class);
		ml.addModule(ClassifierModule.class);
		
		MainMatCalc.main(new PatternDiscoveryInfo(), ml);
	}
}
