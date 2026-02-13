import { useState } from "react";

const quickQuestions = [
    "Check my order status",
    "Return policy",
    "Contact support",
    "Other"
];

export default function AssistantPage() {

    const [messages, setMessages] = useState([
        {
            sender: "bot",
            text: "Hello 👋 I'm your Drawbridge Assistant. How can I help you today?"
        }
    ]);

    const [allowTyping, setAllowTyping] = useState(false);

    const handleQuestion = async (question: string) => {

        setMessages(prev => [...prev, { sender: "user", text: question }]);

        if (question === "Other") {
            setAllowTyping(true);
            setMessages(prev => [
                ...prev,
                { sender: "bot", text: "Please type your issue and we will forward it to support." }
            ]);
            return;
        }

        const res = await fetch("/chatbot", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: question })
        });

        const data = await res.json();

        setMessages(prev => [...prev, { sender: "bot", text: data.reply }]);
    };

    return (
        <div className="p-6 h-full flex flex-col">

            <div className="flex-1 overflow-y-auto space-y-4">

                {messages.map((msg, index) => (
                    <div
                        key={index}
                        className={`p-3 rounded-lg max-w-md ${
                            msg.sender === "bot"
                                ? "bg-gray-200"
                                : "bg-blue-500 text-white ml-auto"
                        }`}
                    >
                        {msg.text}
                    </div>
                ))}

            </div>

            {!allowTyping && (
                <div className="mt-4 grid grid-cols-2 gap-2">

                    {quickQuestions.map(q => (
                        <button
                            key={q}
                            onClick={() => handleQuestion(q)}
                            className="p-3 border rounded-lg hover:bg-gray-100"
                        >
                            {q}
                        </button>
                    ))}

                </div>
            )}

        </div>
    );
}
