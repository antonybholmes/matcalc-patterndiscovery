package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.app;

import org.jebtk.core.AppVersion;
import org.jebtk.modern.UIService;
import org.jebtk.modern.help.GuiAppInfo;

public class PatternDiscoveryInfo extends GuiAppInfo {

  public PatternDiscoveryInfo() {
    super("Pattern Discovery", new AppVersion(1), "Copyright (C) 2016 Antony Holmes",
        UIService.getInstance().loadIcon(PatternDiscoveryIcon.class, 32),
        UIService.getInstance().loadIcon(PatternDiscoveryIcon.class, 128));
  }

}
