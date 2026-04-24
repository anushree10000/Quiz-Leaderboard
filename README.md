Quiz Leaderboard System 
This repository contains a Java-based solution for the Quiz Leaderboard System internship assignment. The application consumes a series of API poll responses, processes participant scores, handles data deduplication, and submits a final sorted leaderboard.Project OverviewThe system is designed to simulate a real-world distributed backend integration problem where API response data may be redundant or inconsistent.

Core Requirements Fulfilled:
1. Polling Logic: The system executes exactly 10 polls to the validator API with a mandatory 5-second delay between each request.

2. Deduplication: Successfully handled duplicate data by using a composite key of roundId + participant to ensure each score is only counted once.
3. Score Aggregation: Aggregated individual round scores into a totalScore per participant.
4. Leaderboard Generation: Produced a sorted leaderboard in descending order based on the total scores.
5. Single Submission: The final leaderboard is submitted to the POST endpoint exactly once.


Technical Implementation
The solution is written in native Java without external libraries to ensure a lightweight footprint.
Networking: Utilizes java.net.http.HttpClient for robust asynchronous communication.
Data Storage: Uses LinkedHashMap to maintain the order of processed events while ensuring $O(1)$ lookup for deduplication.
Logic Flow:Poll 0-9 using the registration number RA2311003010320.

Parse the raw JSON string to extract events.
Filter events based on uniqueness.Aggregate and sort results.Submit the final payload.

Execution Results
The application was successfully executed on macOS (Apple Silicon) with JDK 26.
