package com.xus.mongo2sql.llm;

import java.io.IOException;

public interface ModelClient {
	
	
    String chat(String prompt) throws IOException;
    
    public ModelConfigForRequest getModelConfig();
    
	public void setModelConfig(ModelConfigForRequest modelConfig);

	
}
