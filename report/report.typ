#set par(justify: true)

#show heading: set block(above: 2em, below: 1.2em);
= Introduction

In this assignment under the module ITS66704 Advanced Programming, we are required to utilize JavaFX and LangChain4j to implement a desktop knowledge assistance system powered by Retrieval-Augmented Generation (RAG) technology. The system shall be designed to manage files, build knowledge bases, perform semantic searches, and ultimately respond to user queries through an LLM with appropriate contextual information. In essence, the core of this project is to give us the opportunity to build an AI-driven knowledge system of our own—how exciting indeed.

This report, however, while still maintaining a reasonably formal structure, partially serves as the documentation for a real, functioning codebase. As a result, its tone will be less academically rigid and more aligned with modern engineering documentation—clear, direct, and grounded in practical implementation details. Advanced Programming, being a module that emphasizes practicality above all, should value principles such as “making things work, keeping the logic clear, and ensuring that the design supports the functional requirements” rather than relying solely on meticulously crafted wording that exists only on paper.

Throughout the development of this assignment, instead of following the traditional workflow of completing a full specification document before implementation, we intentionally adopted the reverse approach. Rather than using the conventional waterfall model—where detailed requirements and design specifications are drafted before any actual coding, we embraced a more agile and iterative strategy. The process began with constructing a minimum viable product (MVP) containing essential working features. From there, we continued to expand the system with new functionalities, refactor the codebase, perform optimizations, validate design decisions through real running behaviour, and only then proceed to produce the documentation that reflects the final, working system.

= Requirement Specifications

This section delineates the comprehensive functional and non-functional requirements that guided the development of the RAG system. The requirements were derived from user needs analysis and technical feasibility considerations.

== Functional Requirements

=== FR1: Session Management

*FR1.1 - Session Creation*

- The system shall allow users to create new sessions with user-defined names
- The system shall allow users to select an AI model from a predefined list
- The system shall allow duplicate session names
- The system shall allow users to define session parameters including temperature and top K results, and whether to enable query transformation
- Each session shall be assigned a unique identifier upon creation
- New sessions shall default to the GPT-4o-mini model
- Session creation shall initialize an empty knowledge base and session history

*FR1.2 - Session Selection*

- The system shall display all existing sessions in a sidebar interface
- Users shall be able to select a session by clicking on it
- Upon selection, the system shall load the associated knowledge base and session history
- The main interface shall display the selected session's name and model

*FR1.3 - Session Editing*

- Users shall be able to edit session configurations through a dialog interface
- Users shall be able to change the AI model associated with a session
- Users shall be able to modify session parameters including temperature, top K results, and query transformation settings
- Parameter changes shall trigger reinitialization of the RAG service
- Changes shall be persisted to the database immediately

*FR1.4 - Session Deletion*

- The system shall provide a delete function for sessions
- Deletion shall require explicit user confirmation via dialog
- Deleting a session shall cascade delete all associated messages and embeddings

*FR1.5 - Session Clearing*

- Users shall be able to clear all messages in the current session
- Clearing shall require explicit confirmation via dialog
- The operation shall delete messages from the database and remove them from the UI
- The RAG service's internal session history shall be reset
- The knowledge base shall remain intact

*FR1.6 - Session History Display*

- The system shall display session history in a scrollable chat interface
- User messages shall be visually distinguished from AI responses
- Each message shall include appropriate styling and alignment

*FR1.7 - Session Persistence*

- All messages (user and AI) shall be saved to the database with timestamps
- Session history shall be automatically loaded when a session is selected
- The system shall restore the complete session state from the database

*FR1.8 - Session Exporting*

- Users shall be able to export session history to a text file
- A file chooser dialog shall allow users to specify the export location and filename
- A default filename shall be suggested based on the session name and timestamp
- The exported file shall include session metadata (session name, creation date, model, export date)
- The exported file shall include all messages in chronological order
- The export function shall be accessible via a context menu in the session sidebar
- The system shall prevent exporting if there are no messages in the session
- A confirmation dialog shall inform users of successful export and file location

*FR1.9 - Session Sharing*

- Users shall be able to share session history via Pastebin
- Sharing shall require a valid Pastebin API key configured in the system
- The shared content shall include session metadata and all messages
- The system shall display progress and success dialogs during sharing
- Upon successful upload, the system shall provide the Pastebin URL for copying
- Sharing shall be accessible via a context menu in the session sidebar

=== FR2: Knowledge Base Management

*FR2.1 - Document Import*

- The system shall support import of documents in the following formats: PDF, TXT, DOC, DOCX, PPT, PPTX
- Users shall select documents through a file chooser dialog
- The system shall extract text content from all supported document types
- Extracted text shall be split into chunks for embedding generation

*FR2.2 - Document Preview*

- Users shall be able to view the full text content of any document before import
- The preview shall be displayed in a modal dialog with scrollable text area
- The preview shall accurately represent the text that will be indexed

*FR2.3 - Document Replacement*

- If a document with the same filename already exists, the system shall prompt the user
- Users shall have the option to replace the existing document or cancel the import
- Replacement shall delete old embeddings and generate new ones

*FR2.4 - Document Deletion*

- Users shall be able to remove documents from a knowledge base
- Deletion shall require explicit confirmation
- The system shall remove both the document file and associated embeddings
- Deletion shall update the embedding store index

*FR2.5 - Document Listing*

- The system shall display all documents in the selected session's knowledge base
- Each document entry shall show: filename, file format icon, and action buttons
- The interface shall support scrolling for knowledge bases with many documents

=== FR3: Query Processing & Response Generation

*FR3.1 - Query Input*

- Users shall enter queries through a text input field
- The system shall support multi-line queries
- The send button shall be disabled when the input field is empty
- Queries shall be limited to reasonable length to prevent API errors

*FR3.2 - Contextual Query Understanding*

- The system shall incorporate the last four messages (two exchanges) from session history into the retrieval query
- This contextualized query shall be used for semantic search only
- The original user message shall be preserved for display and re-ranking

*FR3.3 - Semantic Retrieval*

- The system shall embed the contextualized query using OpenAI's text-embedding-3-small model
- The system shall retrieve the top 15 most semantically similar document segments
- Retrieval shall be performed against the session-specific embedding store

*FR3.4 - Hybrid Re-ranking*

- Retrieved segments shall be re-ranked using a hybrid scoring algorithm
- The scoring shall combine: semantic similarity (60%), term frequency (30%), position bonus (5%), exact phrase matching (5%)
- The top 5 segments after re-ranking shall be selected for context augmentation

*FR3.5 - Response Generation*

- The system shall construct a prompt containing the retrieved context segments
- The prompt shall include the full session history for context awareness
- The system shall send the request to the OpenAI Chat API using the selected model
- Responses shall be streamed and displayed in real-time

*FR3.6 - Source Attribution*

- Each response shall include citations of source documents used
- Citations shall display the document filename
- Users shall be able to identify which documents contributed to the response

=== FR5: User Interface Controls

*FR5.1 - Control State Management*

- All input controls shall be disabled during AI response generation
- This includes: message input field, send button, manage knowledge base button, clear session button, and session sidebar
- Controls shall be re-enabled after response completion or error

*FR5.2 - Conditional Visibility*

- The manage knowledge base button shall be hidden when no session is selected
- The clear session button shall be hidden when no session is selected
- Hidden buttons shall not occupy layout space

*FR5.3 - Progress Indicators*

- The system shall display a loading indicator during document processing
- Progress dialogs shall show operation status for long-running tasks
- Users shall receive feedback for all system operations

== Non-Functional Requirements

=== NFR1: Performance

*NFR1.1 - Embedding Cache Efficiency*

- The system shall cache embeddings to avoid redundant API calls
- Only new or modified documents shall trigger embedding generation
- Cache lookup shall be performed before any API call

*NFR1.2 - Memory Optimization*

- Each session shall maintain an isolated embedding store
- Embedding stores shall be loaded on-demand when sessions are selected

=== NFR2: Reliability

*NFR2.1 - Data Persistence*

- All session data, messages, and metadata shall be persisted to SQLite database
- Database operations shall use transactions to ensure consistency
- The system shall handle database connection failures gracefully

*NFR2.2 - Error Handling*

- All API errors shall be caught and reported to the user with descriptive messages
- File I/O errors shall not crash the application
- Invalid document formats shall be rejected with appropriate error messages

*NFR2.3 - Data Integrity*

- Cascading deletes shall ensure no orphaned records in the database
- Foreign key constraints shall be enforced
- The system shall validate user input before database operations

=== NFR3: Usability

*NFR3.1 - User Interface Design*

- The interface shall be clean and intuitive
- All interactive elements shall provide visual feedback on hover and click
- Error messages shall be clear and actionable

*NFR3.2 - Confirmation Dialogs*

- All destructive operations (delete, replace, clear) shall require explicit confirmation
- Confirmation dialogs shall clearly state the consequences of the action
- Users shall have the option to cancel destructive operations

*NFR3.3 - Visual Hierarchy*

- The interface shall use consistent spacing, colors, and typography
- Primary actions shall be visually prominent
- The chat interface shall clearly distinguish user messages from AI responses

=== NFR4: Maintainability

*NFR4.1 - Code Organization*

- The system shall follow MVC architectural pattern
- Business logic shall be separated from UI code
- Commonly used services shall be implemented as singletons for global access whenever appropriate

*NFR4.2 - Configuration Management*

- System constants shall be centralized in a Constants class
- API keys shall be stored securely in environment variables and not hardcoded
- Model configurations shall be easily modifiable

*NFR4.3 - Logging*

- The system shall use Log4j2 for structured logging
- Error logs shall capture stack traces and context information
- Logs shall be written to console during development for real-time monitoring, as well as to files for debugging

=== NFR5: Security & Privacy

*NFR5.1 - API Key Management*

- OpenAI API keys shall be stored in environment variables (.env file)
- The system shall prompt for API key if not configured
- API keys shall not be logged or displayed in plain text
- API key validity shall be verified on application startup through a test request to OpenAI
- Invalid API keys shall trigger an error dialog and disable chat functionality

*NFR5.2 - Data Privacy*

- All user data shall be stored locally on the user's machine
- No data shall be transmitted to third parties except OpenAI for embeddings and generation
- Users shall have full control over their data

*NFR5.3 - Input Validation*

- All user inputs shall be validated before processing
- SQL injection shall be prevented through parameterized queries
- File paths shall be validated to prevent directory traversal attacks

=== NFR6: Scalability

*NFR6.1 - Knowledge Base Size*

- The system shall support knowledge bases with up to 1,000 documents
- Document size shall be limited to 50MB to ensure reasonable processing time

*NFR6.2 - Session History*
- The system shall maintain session history for the lifetime of a session
- Database queries shall remain performant with large message tables

=== NFR7: Compatibility

*NFR7.1 - Platform Support*

- The system shall run on macOS, Windows, and Linux
- JavaFX 26 or higher shall be required
- Java 17 or higher shall be required

*NFR7.2 - Document Format Support*

- The system shall correctly extract text from all specified document formats
- Extraction shall handle various encodings (UTF-8, ASCII, etc.)
- Invalid documents shall be rejected with appropriate messages

*NFR7.3 - API Compatibility*

- The system shall use Langchain4j 1.8.0 for OpenAI integration
- The system shall be compatible with OpenAI API

#pagebreak()

== Use Case Diagram and Descriptions

The following use case diagram illustrates the primary interactions between users and the RAG system. The diagram encompasses all major functional requirements, demonstrating the relationships between actors, use cases, and system boundaries.

#align(center)[
  #image("use-case.png", width: 50%)
]
