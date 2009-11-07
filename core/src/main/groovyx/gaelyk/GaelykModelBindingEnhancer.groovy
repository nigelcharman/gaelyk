package groovyx.gaelyk;

import groovy.lang.Binding;

public class GaelykModelBindingEnhancer {
	private Binding binding
	private static final String GAELYK_MODEL = "gaelyk.model"
	
	public GaelykModelBindingEnhancer(Binding binding) {
		this.binding = binding;
	}
	
	public void template() {
		def model = binding.request.getAttribute(GAELYK_MODEL)
		model.each { key, value ->
			binding.setVariable(key, value)
		}
	}
	
	public void servlet() {
		binding.request.setAttribute(GAELYK_MODEL, [:])
		binding.setVariable("model", binding.request.getAttribute(GAELYK_MODEL))
	}
}
