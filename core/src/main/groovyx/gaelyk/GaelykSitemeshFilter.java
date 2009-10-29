package groovyx.gaelyk;

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;

public class GaelykSitemeshFilter extends ConfigurableSiteMeshFilter {
  @Override
  protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
    builder.setCustomDecoratorSelector(new MetaTagBasedDecoratorSelector("/WEB-INF/includes/%s.gtpl"));
  }
}
