package groovyx.gaelyk;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.sitemesh.config.LinkTagBasedDecoratorSelector;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.ExportTagToContentRule;
import org.sitemesh.content.tagrules.html.LinkTagWithExcludesTagRule;
import org.sitemesh.content.tagrules.html.MetaTagRule;
import org.sitemesh.tagprocessor.State;
import org.sitemesh.tagprocessor.StateTransitionRule;

public class GaelykSitemeshFilter extends ConfigurableSiteMeshFilter {

  public class LinkHtmlTagRuleBundle implements TagRuleBundle {

    public void install(State defaultState, ContentProperty contentProperty,
        SiteMeshContext siteMeshContext) {
      // Core rules for SiteMesh to be functional.
      defaultState.addRule("head", new ExportTagToContentRule(contentProperty
          .getChild("head"), false));
      defaultState.addRule("title", new ExportTagToContentRule(contentProperty
          .getChild("title"), false));
      defaultState.addRule("body", new ExportTagToContentRule(contentProperty
          .getChild("body"), false));
      defaultState.addRule("meta", new MetaTagRule(contentProperty
          .getChild("meta")));
      defaultState.addRule("link", new LinkTagWithExcludesTagRule(
          contentProperty.getChild("link"),
          new String[] { "layout" }));

      defaultState.addRule("xml", new StateTransitionRule(new State()));
    }

    public void cleanUp(State defaultState, ContentProperty contentProperty,
        SiteMeshContext siteMeshContext) {
      // In the event that no <body> tag was captured, use the default buffer
      // contents instead
      // (i.e. the whole document, except anything that was written to other
      // buffers).
      if (!contentProperty.getChild("body").hasValue()) {
        contentProperty.getChild("body").setValue(contentProperty.getValue());
      }
    }
  }
  @Override
  protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
    builder.setCustomDecoratorSelector(new LinkTagBasedDecoratorSelector("/WEB-INF/includes/%s.gtpl"));
    builder.setTagRuleBundles(new LinkHtmlTagRuleBundle(),new DecoratorTagRuleBundle());
  }
}
