import React from "react";

export default class RuntimeErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error("Application render error:", error, info);
  }

  render() {
    if (this.state.error) {
      return (
        <main dir="rtl" style={{ fontFamily: "sans-serif", padding: 24, color: "#222", lineHeight: 1.8 }}>
          <h1 style={{ fontSize: 22, fontWeight: 700 }}>خطا در نمایش برنامه</h1>
          <p>برنامه اجرا شده، اما هنگام نمایش یک خطای JavaScript رخ داده است.</p>
          <details open style={{ marginTop: 16, whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>
            <summary>جزئیات خطا برای عیب‌یابی</summary>
            <code>{String(this.state.error?.stack || this.state.error?.message || this.state.error)}</code>
          </details>
          <button
            onClick={() => window.location.reload()}
            style={{ marginTop: 20, padding: "10px 16px", border: "1px solid #999", borderRadius: 8 }}
          >
            بارگذاری دوباره
          </button>
        </main>
      );
    }
    return this.props.children;
  }
}
