import { useState, useRef, useEffect } from "react";
import { Bot } from "lucide-react";

interface Message {
  sender: "bot" | "user";
  text: string;
}

const quickOptions = [
  { label: "Track my order" },
  { label: "Request a refund" },
  { label: "Technical issue" },
  { label: "Pricing & subscription" },
  { label: "Other" }
];

const intentMap: Record<string, string> = {
  "Track my order": "track_order",
  "Request a refund": "refund",
  "Technical issue": "technical_issue",
  "Pricing & subscription": "pricing",
  "Other": "other"
};

const AssistantPage = () => {
  const [messages, setMessages] = useState<Message[]>([
    {
      sender: "bot",
      text: "Hello! I'm your virtual assistant. Please select one of the options below so I can assist you."
    }
  ]);

  const [showOptions, setShowOptions] = useState(false);
  const [isTyping, setIsTyping] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  const handleOptionClick = async (label: string) => {
    setMessages(prev => [...prev, { sender: "user", text: label }]);
    setShowOptions(false);
    setIsTyping(true);

    try {
      const token = localStorage.getItem("drawbridge_token");

      const res = await fetch("http://localhost:8080/api/assistant", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify({
          intent: intentMap[label] ?? "other"
        })
      });

      if (!res.ok) {
        const errText = await res.text();
        setIsTyping(false);
        setMessages(prev => [
          ...prev,
          { sender: "bot", text: `Assistant service error (${res.status}). ${errText}` }
        ]);
        return;
      }

      const data = await res.json();

      setIsTyping(false);
      setMessages(prev => [...prev, { sender: "bot", text: data.reply ?? "No reply returned." }]);
    } catch (e) {
      setIsTyping(false);
      setMessages(prev => [...prev, { sender: "bot", text: "Unable to connect to assistant service." }]);
    }
  };

  return (
    <div className="h-screen flex flex-col bg-gray-50">
      {/* Header */}
      <div className="p-4 border-b bg-white flex items-center gap-3">
        <div className="w-10 h-10 rounded-full bg-blue-600 flex items-center justify-center">
          <Bot className="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 className="font-semibold text-lg">Assistant</h1>
          <p className="text-xs text-gray-500">Online</p>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 p-6 overflow-y-auto space-y-4">
        {messages.map((msg, index) => (
          <div
            key={index}
            className={`flex ${msg.sender === "user" ? "justify-end" : "justify-start"}`}
          >
            <div
              className={`max-w-md px-4 py-3 rounded-2xl text-sm shadow-sm transition whitespace-pre-line ${
                msg.sender === "user"
                  ? "bg-blue-600 text-white"
                  : "bg-white border text-gray-800"
              }`}
            >
              {msg.text}
            </div>
          </div>
        ))}

        {isTyping && (
          <div className="flex justify-start">
            <div className="bg-white border px-4 py-3 rounded-2xl text-sm text-gray-500 shadow-sm">
              Assistant is typing...
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Fake Input */}
      <div className="p-4 border-t bg-white">
        <div
          onClick={() => setShowOptions(prev => !prev)}
          className="w-full border rounded-xl px-4 py-3 text-gray-400 cursor-pointer hover:bg-gray-50 transition"
        >
          Type your message...
        </div>
      </div>

      {/* Quick Options */}
      {showOptions && (
        <div className="p-4 border-t bg-gray-50 grid gap-3">
          {quickOptions.map((option, index) => (
            <button
              key={index}
              onClick={() => handleOptionClick(option.label)}
              className="bg-white border rounded-xl px-4 py-3 text-left hover:bg-gray-100 transition shadow-sm"
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

export default AssistantPage;