package edu.columbia.rdf.matcalc.toolbox.patterndiscovery.app;

import org.jebtk.core.AppVersion;
import org.jebtk.modern.AssetService;
import org.jebtk.modern.help.GuiAppInfo;

public class PatternDiscoveryInfo extends GuiAppInfo {

  public PatternDiscoveryInfo() {
    super("Pattern Discovery", new AppVersion(1),
        "Copyright (C) 2016 Antony Holmes",
        AssetService.getInstance().loadIcon(PatternDiscoveryIcon.class, 32),
        AssetService.getInstance().loadIcon(PatternDiscoveryIcon.class, 128));
  }

}
