<center><h1 align="center">🤖 RAGent</h1></center>

<p align="center">A powerful Retrieval-Augmented Generation (RAG) chatbot application built with JavaFX</p>

## The Problem

When working with large knowledge bases and document collections, getting accurate and contextual answers from AI can be challenging. Traditional chatbots don't have access to your specific documents, and manually searching through files is time-consuming and inefficient.

## The Solution

RAGent combines the power of AI language models with intelligent document retrieval. It indexes your knowledge base, understands your questions in context, retrieves relevant information, and generates accurate responses based on your actual documents - all through an elegant, native desktop interface.

## ✨ Features

- **📚 Knowledge Base Management** - Index and organize multiple document collections
- **🔍 Advanced RAG Pipeline** - Multi-query retrieval with query transformation
- **🎯 Smart Reranking** - Combines embedding similarity with RRF and MMR for optimal results
- **💬 Contextual Chat** - Maintains conversation history for coherent multi-turn dialogues
- **📊 Message Details** - View context references and query variations for each response
- **🎨 Modern UI** - Clean, dark-mode togglable interface built with JavaFX
- **🔌 Multi-Provider Support** - Works with OpenAI, Groq, and other LLM providers
- **⚙️ Customizable** - Adjust temperature, top-k, and enable/disable query transformation

## 🖥 Screenshots

<img width="48%" alt="image" src="https://github.com/user-attachments/assets/36d18ab0-1cf2-4a52-9639-c2bbb4030ba7" />
<img width="48%" alt="image" src="https://github.com/user-attachments/assets/e5725ebf-c1d9-4b2e-812c-3f8ce8ea6882" />


## 🔬 Technologies Used

![skills](https://img.shields.io/badge/-Java-FF0000?style=for-the-badge&logo=openjdk&logoColor=white&color=orange)
![skills](https://img.shields.io/badge/-JavaFX-FF0000?style=for-the-badge&logo=java&logoColor=white&color=blue)
![skills](https://img.shields.io/badge/-LangChain4j-FF0000?style=for-the-badge&logo=chainlink&logoColor=white&color=purple)
![skills](https://img.shields.io/badge/-SQLite-FF0000?style=for-the-badge&logo=sqlite&logoColor=white&color=blue)
![skills](https://img.shields.io/badge/-Maven-FF0000?style=for-the-badge&logo=apache-maven&logoColor=white&color=red)

## ⌨️ Setup

If you want to run the application on your local machine:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/melvinchia3636/rag-assignment
   cd rag-assignment
   ```

2. **Set up API keys:**
   - Copy `.env.example` to `.env`
   - Add your API keys.

3. **Build the project:**
   ```bash
   mvn clean package
   ```

4. **Run the application:**
   ```bash
   java -jar target/rag-1.0-SNAPSHOT-standalone.jar
   ```

## 📈 Status

This project is mostly completed. Core features are complete and functional. If any bugs are found, please file an issue, and I'll resolve it ASAP. Contributions are welcome!

## 💡 Inspirations

This project was inspired by the group assignment of the ITS66704 Advanced Programming course at Taylor's University. This is far beyond the final version submitted for the assignment, as I am afraid that the teacher might doubt the authenticity of such a sophisticated project and also the work distribution if it were to be a group project. After all, it's quite hard to believe that a single first-year undergraduate student could have developed such an advanced RAG application all by himself within less than a week.

## 📄 License

Copyright © 2025 Melvin Chia<br/>
Licensed under MIT.
