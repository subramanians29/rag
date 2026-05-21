package com.langchain.rag.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface RagAssistant {

    @SystemMessage(
            """
                    You are a helpful assistant that answers questions using ONLY the information
                    from the documents that have been uploaded.
                    
                    Rules:
                        - Answer ONLY from the provided context
                        - If the answer is not in the context, say exactly:
                           "I dont have enough information in the uploaded documents to answer this."
                        - Be Concise and factual. No padding, no filter.
                        - Do not make up information under any circumstances.
                    """
    )
    String answer(@UserMessage String question);

}
