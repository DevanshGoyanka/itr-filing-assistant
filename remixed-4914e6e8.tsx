import { useState, useRef, useMemo, useCallback } from "react";

/* ═══════════════════════════════════════════════════════════════════════════
   MYTAXERP  —  Complete Income Tax CA Practice ERP
   Modules: Dashboard · Client Master · ITR Computation · TDS ·
            Office · Billing · Reports
═══════════════════════════════════════════════════════════════════════════ */

const MOCK_CLIENTS = [
  { id:1,  name:"Rajesh Kumar Sharma",    pan:"ABCRS1234F", dob:"1978-06-14", status:"Individual", age:46, city:"Mumbai",     state:"Maharashtra", mobile:"9845001234", email:"rajesh@gmail.com",   itrType:"ITR-1", itrStatus:"Filed",       assignedTo:"Priya", group:"Salary",   tdsAmt:142000, advance:0,      fatherName:"Ram Prasad Sharma",  regime:"O", pincode:"400001" },
  { id:2,  name:"Priya Venkataraman",     pan:"DCPRV5678G", dob:"1990-11-02", status:"Individual", age:34, city:"Chennai",    state:"Tamil Nadu",   mobile:"9912345678", email:"priya@outlook.com",  itrType:"ITR-2", itrStatus:"Pending",     assignedTo:"Amit",  group:"Salary",   tdsAmt:88000,  advance:0,      fatherName:"K. Venkataraman",    regime:"N", pincode:"600001" },
  { id:3,  name:"Apex Traders LLP",       pan:"AABFA2345H", dob:"2012-04-01", status:"LLP",        age:0,  city:"Delhi",      state:"Delhi",        mobile:"9011223344", email:"accounts@apex.in",   itrType:"ITR-5", itrStatus:"In Progress", assignedTo:"Priya", group:"Business", tdsAmt:0,      advance:95000,  fatherName:"",                   regime:"O", pincode:"110001" },
  { id:4,  name:"Sunita Devi Agarwal",    pan:"BKPSA9988K", dob:"1953-03-22", status:"Individual", age:71, city:"Jaipur",     state:"Rajasthan",    mobile:"9765432100", email:"sunita@yahoo.com",   itrType:"ITR-1", itrStatus:"Filed",       assignedTo:"Amit",  group:"Salary",   tdsAmt:34000,  advance:0,      fatherName:"Mohan Lal Gupta",    regime:"O", pincode:"302001" },
  { id:5,  name:"Harish Chandra Gupta",   pan:"FXPHG6754L", dob:"1969-09-30", status:"Individual", age:55, city:"Lucknow",    state:"Uttar Pradesh",mobile:"9988776655", email:"harish@biz.net",     itrType:"ITR-3", itrStatus:"Pending",     assignedTo:"Priya", group:"Business", tdsAmt:210000, advance:75000,  fatherName:"Chandra Bhan Gupta", regime:"O", pincode:"226001" },
  { id:6,  name:"Global Exports Pvt Ltd", pan:"AAACG3456P", dob:"2005-07-12", status:"Company",   age:0,  city:"Bengaluru",  state:"Karnataka",    mobile:"9871234567", email:"fin@global.co.in",   itrType:"ITR-6", itrStatus:"Pending",     assignedTo:"Rajan", group:"Company",  tdsAmt:0,      advance:450000, fatherName:"",                   regime:"O", pincode:"560001" },
  { id:7,  name:"Deepak Ramesh Nair",     pan:"CALPN8821Q", dob:"1985-01-17", status:"Individual", age:39, city:"Pune",       state:"Maharashtra",  mobile:"9654321098", email:"deepak@infosys.com", itrType:"ITR-2", itrStatus:"Filed",       assignedTo:"Amit",  group:"Salary",   tdsAmt:178000, advance:50000,  fatherName:"Ramesh Nair",        regime:"N", pincode:"411001" },
  { id:8,  name:"Kavitha Subramaniam",    pan:"HBPKS7712R", dob:"1962-08-05", status:"Individual", age:62, city:"Coimbatore", state:"Tamil Nadu",   mobile:"9543210987", email:"kavitha@hm.com",     itrType:"ITR-1", itrStatus:"Filed",       assignedTo:"Rajan", group:"Salary",   tdsAmt:45000,  advance:0,      fatherName:"T.R. Subramaniam",   regime:"O", pincode:"641001" },
  { id:9,  name:"Pioneer Industries HUF", pan:"AAAPP4567S", dob:"1998-04-01", status:"HUF",        age:0,  city:"Ahmedabad",  state:"Gujarat",      mobile:"9432109876", email:"huf@pioneer.in",     itrType:"ITR-2", itrStatus:"In Progress", assignedTo:"Priya", group:"HUF",      tdsAmt:65000,  advance:30000,  fatherName:"",                   regime:"O", pincode:"380001" },
  { id:10, name:"Amit Suresh Joshi",      pan:"GCPAJ5543T", dob:"1993-12-28", status:"Individual", age:31, city:"Hyderabad",  state:"Telangana",    mobile:"9321098765", email:"amit@wipro.com",     itrType:"ITR-1", itrStatus:"Pending",     assignedTo:"Rajan", group:"Salary",   tdsAmt:62000,  advance:0,      fatherName:"Suresh Joshi",       regime:"N", pincode:"500001" },
];

const TASKS = [
  { id:1, client:"Rajesh Kumar Sharma",  task:"Verify Form 16 with 26AS",           due:"2024-07-25", priority:"High",   status:"Done",    assigned:"Priya" },
  { id:2, client:"Priya Venkataraman",   task:"Capital gains – equity scrip-wise",  due:"2024-07-28", priority:"High",   status:"Open",    assigned:"Amit"  },
  { id:3, client:"Apex Traders LLP",     task:"Prepare P&L and Balance Sheet",      due:"2024-09-30", priority:"Medium", status:"Open",    assigned:"Priya" },
  { id:4, client:"Harish Chandra Gupta", task:"Collect 80C investment proofs",       due:"2024-07-20", priority:"High",   status:"Overdue", assigned:"Priya" },
  { id:5, client:"Global Exports Pvt",   task:"Tax Audit – Form 3CA/3CD",           due:"2024-09-30", priority:"High",   status:"Open",    assigned:"Rajan" },
  { id:6, client:"Deepak Ramesh Nair",   task:"Foreign Income – Schedule FSI",      due:"2024-07-31", priority:"Medium", status:"Done",    assigned:"Amit"  },
  { id:7, client:"Pioneer Industries",   task:"Advance tax challan – Q2 Sep 2024",  due:"2024-09-15", priority:"High",   status:"Open",    assigned:"Priya" },
];

const DEADLINES = [
  { date:"Jul 31", label:"ITR Filing Deadline (Non-Audit)",  color:"#dc2626", urgent:true  },
  { date:"Sep 15", label:"Advance Tax – 2nd Instalment",     color:"#d97706", urgent:false },
  { date:"Sep 30", label:"Tax Audit Report (3CA/3CD/3CB)",   color:"#dc2626", urgent:false },
  { date:"Oct 31", label:"ITR Filing – Audit Cases",         color:"#7c3aed", urgent:false },
  { date:"Dec 15", label:"Advance Tax – 3rd Instalment",     color:"#d97706", urgent:false },
  { date:"Mar 15", label:"Advance Tax – 4th Instalment",     color:"#d97706", urgent:false },
  { date:"Mar 31", label:"Vivad se Vishwas / Belated ITR",   color:"#dc2626", urgent:false },
];

const STATUS_STYLE = {
  "Filed":       { bg:"#dcfce7", color:"#15803d", dot:"#16a34a" },
  "Pending":     { bg:"#fef9c3", color:"#92400e", dot:"#d97706" },
  "In Progress": { bg:"#dbeafe", color:"#1e40af", dot:"#3b82f6" },
  "Overdue":     { bg:"#fee2e2", color:"#991b1b", dot:"#dc2626" },
};
const STATUS_CHIP = {
  Individual:{ bg:"#eff6ff", color:"#1d4ed8", brd:"#bfdbfe" },
  LLP:       { bg:"#f0fdf4", color:"#15803d", brd:"#bbf7d0" },
  Company:   { bg:"#fff7ed", color:"#c2410c", brd:"#fed7aa" },
  HUF:       { bg:"#faf5ff", color:"#7e22ce", brd:"#e9d5ff" },
};

/* ─── helpers ─────────────────────────────────────────────────────────── */
const n   = v => Number(v) || 0;
const fmt = v => v && !isNaN(v) && Number(v) !== 0 ? Number(v).toLocaleString("en-IN") : "";
const fmtCr = v => { const a = Number(v)||0; return a>=10000000?`₹${(a/10000000).toFixed(2)}Cr`:a>=100000?`₹${(a/100000).toFixed(1)}L`:`₹${a.toLocaleString("en-IN")}`; };

function Chip({ label }) {
  const s = STATUS_CHIP[label] || { bg:"#f1f5f9", color:"#475569", brd:"#e2e8f0" };
  return <span style={{ fontSize:10, fontWeight:700, background:s.bg, color:s.color, border:`1px solid ${s.brd}`, padding:"1px 7px", borderRadius:20 }}>{label}</span>;
}
function StatusBadge({ status }) {
  const s = STATUS_STYLE[status] || STATUS_STYLE["Pending"];
  return <span style={{ display:"inline-flex", alignItems:"center", gap:4, fontSize:10.5, fontWeight:600, background:s.bg, color:s.color, padding:"2px 8px", borderRadius:20 }}>
    <span style={{ width:6, height:6, borderRadius:"50%", background:s.dot, display:"inline-block" }} />{status}
  </span>;
}
function Card({ children, style={}, onClick }) {
  return <div onClick={onClick} style={{ background:"#fff", borderRadius:10, border:"1px solid #e8edf5", ...style }}>{children}</div>;
}
function Metric({ label, value, sub, color="#1565c0", icon }) {
  return (
    <Card style={{ padding:"14px 16px", flex:1 }}>
      <div style={{ display:"flex", justifyContent:"space-between", alignItems:"flex-start" }}>
        <div>
          <div style={{ fontSize:10.5, fontWeight:600, color:"#94a3b8", textTransform:"uppercase", letterSpacing:"0.06em", marginBottom:6 }}>{label}</div>
          <div style={{ fontSize:22, fontWeight:800, color, fontFamily:"'Courier New',monospace", lineHeight:1 }}>{value}</div>
          {sub && <div style={{ fontSize:11, color:"#64748b", marginTop:4 }}>{sub}</div>}
        </div>
        {icon && <div style={{ fontSize:24, opacity:0.18 }}>{icon}</div>}
      </div>
    </Card>
  );
}

/* ─── TAX ENGINE ─────────────────────────────────────────────────────── */
function computeTax(d, forceRegime, client) {
  const isNew       = forceRegime === "N";
  const age         = client?.age || 0;
  const isSenior    = age >= 60;
  const isSuperSr   = age >= 80;

  const grossSal    = n(d.salary) + n(d.perq);
  const stdDed      = grossSal > 0 ? (isNew ? 75000 : 50000) : 0;
  const profTax     = !isNew ? n(d.profTax) : 0;
  const salaryInc   = Math.max(0, grossSal - stdDed - profTax);

  const nav         = Math.max(0, n(d.grossRent) - n(d.muniTax));
  const hp30        = nav > 0 ? Math.round(nav * 0.3) : 0;
  const hpInc       = nav - hp30 - n(d.housingInt);

  const stcg111A    = n(d.stcg111A), stcgOth = n(d.stcgOther);
  const ltcg112A    = n(d.ltcg112A), ltcg20  = n(d.ltcg20);
  const totalSTCG   = stcg111A + stcgOth;
  const totalLTCG   = Math.max(0, ltcg112A + ltcg20 - n(d.ltcgExempt));
  const sbInt       = n(d.sbInt), fdInt = n(d.fdInt);
  const otherSrc    = sbInt + fdInt + n(d.othSrc);
  const gti         = salaryInc + hpInc + totalSTCG + totalLTCG + otherSrc;

  const auto80TTA   = !isNew && !isSenior ? Math.min(sbInt, 10000)              : 0;
  const auto80TTB   = !isNew && isSenior  ? Math.min(sbInt + fdInt, 50000)      : 0;
  const sec80C      = !isNew ? Math.min(n(d.sec80C),    150000) : 0;
  const sec80CCD1B  = !isNew ? Math.min(n(d.sec80CCD1B), 50000) : 0;
  const sec80D      = !isNew ? n(d.sec80D) : 0;
  const sec80E      = !isNew ? n(d.sec80E) : 0;
  const sec80G      = !isNew ? n(d.sec80G) : 0;
  const totalDed    = sec80C + sec80CCD1B + sec80D + sec80E + sec80G + auto80TTA + auto80TTB;

  const normalInc   = Math.max(0, gti - (stcg111A + ltcg112A) - totalDed);
  const taxableInc  = Math.max(0, normalInc + stcg111A + ltcg112A);
  const basicExempt = isSuperSr ? 500000 : isSenior ? 300000 : 250000;

  let normalTax = 0;
  if (!isNew) {
    if (normalInc > basicExempt) {
      if (normalInc <= 500000)       normalTax = (normalInc - basicExempt) * 0.05;
      else if (normalInc <= 1000000) normalTax = (500000-basicExempt)*0.05 + (normalInc-500000)*0.20;
      else                           normalTax = (500000-basicExempt)*0.05 + 500000*0.20 + (normalInc-1000000)*0.30;
    }
  } else {
    if      (normalInc <= 300000)  normalTax = 0;
    else if (normalInc <= 600000)  normalTax = (normalInc-300000)*0.05;
    else if (normalInc <= 900000)  normalTax = 15000  + (normalInc-600000)*0.10;
    else if (normalInc <= 1200000) normalTax = 45000  + (normalInc-900000)*0.15;
    else if (normalInc <= 1500000) normalTax = 90000  + (normalInc-1200000)*0.20;
    else                           normalTax = 150000 + (normalInc-1500000)*0.30;
  }

  const stcgTax      = stcg111A * 0.15;
  const ltcgExSlice  = Math.max(0, 100000 - Math.max(0, normalInc - basicExempt));
  const ltcgTaxable  = Math.max(0, ltcg112A - ltcgExSlice);
  const ltcgTax      = ltcgTaxable * 0.10;
  const ltcg20Tax    = ltcg20 * 0.20;
  const beforeRebate = normalTax + stcgTax + ltcgTax + ltcg20Tax;

  const rebate87A    = !isNew
    ? (taxableInc <= 500000 ? Math.min(beforeRebate, 12500) : 0)
    : (taxableInc <= 700000 ? Math.min(beforeRebate, 25000) : 0);

  const taxAfterRebate = Math.max(0, beforeRebate - rebate87A);
  const srRate         = taxableInc>50000000?0.37:taxableInc>20000000?0.25:taxableInc>10000000?0.15:taxableInc>5000000?0.10:0;
  const surcharge      = taxAfterRebate * srRate;
  const cess           = (taxAfterRebate + surcharge) * 0.04;
  const grossTaxLiab   = Math.round(taxAfterRebate + surcharge + cess);
  const totalPaid      = n(d.tds26AS) + n(d.tds16) + n(d.advTax) + n(d.selfTax);
  const balance        = grossTaxLiab - totalPaid;

  return {
    grossSal, stdDed, profTax, salaryInc, nav, hp30, hpInc,
    totalSTCG, totalLTCG, otherSrc, gti,
    auto80TTA, auto80TTB, sec80C, sec80CCD1B, sec80D, sec80E, sec80G, totalDed,
    taxableInc, normalTax, stcgTax, ltcgTax, ltcg20Tax, rebate87A, surcharge, cess,
    grossTaxLiab, totalPaid, balance,
    isRefund: balance < 0, isSenior, isSuperSr, isNew,
    stdDedNote: isNew ? "₹75K – New" : "₹50K – Old"
  };
}

/* ─── Share Modal ─────────────────────────────────────────────────────── */
function ShareModal({ client, tax, onClose }) {
  const [mode, setMode]   = useState("email");
  const [sent, setSent]   = useState(false);
  if (!client) return null;

  const summary = `ITR Computation Summary – AY 2024-25
Client: ${client.name}
PAN: ${client.pan}
Taxable Income: ₹ ${tax?.taxableInc?.toLocaleString("en-IN") || 0}
Tax Liability: ₹ ${tax?.grossTaxLiab?.toLocaleString("en-IN") || 0}
${tax?.isRefund ? `Refund Due: ₹ ${Math.abs(tax?.balance||0).toLocaleString("en-IN")}` : tax?.balance > 0 ? `Tax Payable: ₹ ${tax.balance.toLocaleString("en-IN")}` : "Nil Balance"}
Regime: ${tax?.isNew ? "New (115BAC)" : "Old"}

Prepared by: Your CA Firm
This is a system-generated computation.`;

  const waMsg = encodeURIComponent(summary);
  const emailBody = encodeURIComponent(summary);
  const emailSubj = encodeURIComponent(`ITR Computation AY 2024-25 – ${client.name}`);

  const send = () => {
    if (mode === "email") window.open(`mailto:${client.email}?subject=${emailSubj}&body=${emailBody}`);
    else window.open(`https://wa.me/91${client.mobile}?text=${waMsg}`);
    setSent(true);
  };

  return (
    <div style={{ position:"fixed", inset:0, background:"rgba(0,0,0,0.55)", zIndex:9999, display:"flex", alignItems:"center", justifyContent:"center" }}>
      <div style={{ background:"#fff", borderRadius:12, width:480, boxShadow:"0 20px 60px rgba(0,0,0,0.3)", overflow:"hidden" }}>
        {/* Header */}
        <div style={{ background:"#0f172a", padding:"14px 18px", display:"flex", justifyContent:"space-between", alignItems:"center" }}>
          <span style={{ fontSize:13.5, fontWeight:700, color:"#e2eaf6" }}>📤 Send Computation to Client</span>
          <button onClick={onClose} style={{ background:"none", border:"none", color:"#5a7a9a", cursor:"pointer", fontSize:18 }}>✕</button>
        </div>
        {/* Client info */}
        <div style={{ padding:"14px 18px", borderBottom:"1px solid #f1f5f9", background:"#f8fafc", display:"flex", gap:10, alignItems:"center" }}>
          <div style={{ width:40, height:40, borderRadius:10, background:"#e0e7ff", display:"flex", alignItems:"center", justifyContent:"center", fontSize:18 }}>👤</div>
          <div>
            <div style={{ fontSize:13, fontWeight:700, color:"#0f172a" }}>{client.name}</div>
            <div style={{ fontSize:11, color:"#64748b" }}>{client.email}  ·  +91 {client.mobile}</div>
          </div>
        </div>
        {/* Mode select */}
        <div style={{ padding:"16px 18px" }}>
          <div style={{ fontSize:11, fontWeight:700, color:"#94a3b8", textTransform:"uppercase", letterSpacing:"0.06em", marginBottom:10 }}>Send via</div>
          <div style={{ display:"flex", gap:10, marginBottom:16 }}>
            {[
              { id:"email", icon:"✉️", label:"Email",     sub:client.email  },
              { id:"wa",    icon:"💬", label:"WhatsApp",  sub:`+91 ${client.mobile}` },
            ].map(m => (
              <div key={m.id} onClick={() => setMode(m.id)} style={{ flex:1, border:`2px solid ${mode===m.id?"#1565c0":"#e2e8f0"}`, borderRadius:8, padding:"10px 12px", cursor:"pointer", background:mode===m.id?"#eff6ff":"#fff", transition:"all 0.15s" }}>
                <div style={{ fontSize:20, marginBottom:4 }}>{m.icon}</div>
                <div style={{ fontSize:12.5, fontWeight:700, color:mode===m.id?"#1565c0":"#374151" }}>{m.label}</div>
                <div style={{ fontSize:11, color:"#94a3b8" }}>{m.sub}</div>
              </div>
            ))}
          </div>

          {/* Preview */}
          <div style={{ background:"#f8fafc", border:"1px solid #e2e8f0", borderRadius:8, padding:"10px 12px", marginBottom:16, fontSize:11.5, color:"#374151", whiteSpace:"pre-line", lineHeight:1.7, maxHeight:160, overflowY:"auto", fontFamily:"'Courier New',monospace" }}>
            {summary}
          </div>

          {sent
            ? <div style={{ background:"#dcfce7", border:"1px solid #86efac", borderRadius:8, padding:"10px 14px", textAlign:"center", fontSize:12.5, fontWeight:600, color:"#15803d" }}>
                ✓ {mode === "email" ? "Email client opened" : "WhatsApp opened"} — complete sending in the app
              </div>
            : <div style={{ display:"flex", gap:8 }}>
                <button onClick={onClose} style={{ flex:1, padding:"9px", background:"#f1f5f9", border:"none", borderRadius:7, cursor:"pointer", fontSize:12.5, fontWeight:600, color:"#475569" }}>Cancel</button>
                <button onClick={send} style={{ flex:2, padding:"9px", background:mode==="email"?"#1565c0":"#16a34a", border:"none", borderRadius:7, cursor:"pointer", fontSize:12.5, fontWeight:700, color:"#fff" }}>
                  {mode === "email" ? "✉️ Open Email Draft" : "💬 Open WhatsApp Chat"}
                </button>
              </div>
          }
        </div>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════════
   DASHBOARD
══════════════════════════════════════════════════════════════════════════ */
function DashboardModule({ clients, onNav }) {
  const filed   = clients.filter(c=>c.itrStatus==="Filed").length;
  const pending = clients.filter(c=>c.itrStatus==="Pending").length;
  const inProg  = clients.filter(c=>c.itrStatus==="In Progress").length;

  return (
    <div style={{ padding:"18px 22px", overflowY:"auto", height:"100%" }}>
      <div style={{ display:"flex", gap:12, marginBottom:18 }}>
        <Metric label="Total Clients" value={clients.length} sub="AY 2024-25" icon="👥" />
        <Metric label="ITR Filed" value={filed} sub={`${Math.round(filed/clients.length*100)}% complete`} color="#15803d" icon="✅" />
        <Metric label="Pending / In Progress" value={pending + inProg} sub={`${pending} pending · ${inProg} in progress`} color="#d97706" icon="⏳" />
        <Metric label="Total TDS Pool" value={fmtCr(clients.reduce((s,c)=>s+c.tdsAmt,0))} sub="Salary clients" color="#7c3aed" icon="💰" />
      </div>

      <div style={{ display:"grid", gridTemplateColumns:"1.6fr 1fr 1fr", gap:14 }}>
        {/* Filing Status Table */}
        <Card>
          <div style={{ padding:"11px 16px", borderBottom:"1px solid #f1f5f9", display:"flex", justifyContent:"space-between", alignItems:"center" }}>
            <span style={{ fontWeight:700, fontSize:13, color:"#0f172a" }}>Client Filing Status</span>
            <button onClick={()=>onNav("clients")} style={{ background:"none", border:"none", color:"#3b82f6", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>View All →</button>
          </div>
          <div style={{ maxHeight:310, overflowY:"auto" }}>
            <table style={{ width:"100%", borderCollapse:"collapse" }}>
              <thead><tr style={{ background:"#f8fafc" }}>
                {["Client","ITR","Assigned","Status"].map(h=>(
                  <th key={h} style={{ padding:"6px 10px", textAlign:"left", fontSize:10, fontWeight:700, color:"#94a3b8", letterSpacing:"0.05em", textTransform:"uppercase", borderBottom:"1px solid #f1f5f9" }}>{h}</th>
                ))}
              </tr></thead>
              <tbody>
                {clients.map(c=>(
                  <tr key={c.id} style={{ borderTop:"1px solid #f8fafc" }} onMouseEnter={e=>e.currentTarget.style.background="#f8fafc"} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
                    <td style={{ padding:"6px 10px" }}>
                      <div style={{ fontSize:12, fontWeight:600, color:"#0f172a" }}>{c.name.split(" ").slice(0,2).join(" ")}</div>
                      <div style={{ fontSize:10, color:"#94a3b8" }}>{c.pan}</div>
                    </td>
                    <td style={{ padding:"6px 10px" }}><span style={{ fontSize:10, fontWeight:700, color:"#1d4ed8", background:"#eff6ff", padding:"2px 6px", borderRadius:4 }}>{c.itrType}</span></td>
                    <td style={{ padding:"6px 10px", fontSize:11, color:"#64748b" }}>{c.assignedTo}</td>
                    <td style={{ padding:"6px 10px" }}><StatusBadge status={c.itrStatus} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        {/* Compliance Calendar */}
        <Card>
          <div style={{ padding:"11px 16px", borderBottom:"1px solid #f1f5f9" }}>
            <span style={{ fontWeight:700, fontSize:13, color:"#0f172a" }}>📅 Compliance Calendar</span>
          </div>
          <div style={{ padding:"8px 14px" }}>
            {DEADLINES.map((d,i)=>(
              <div key={i} style={{ display:"flex", gap:8, alignItems:"center", padding:"5px 0", borderBottom:"1px solid #f8fafc" }}>
                <div style={{ minWidth:42, fontSize:10.5, fontWeight:800, color:d.color, background:`${d.color}15`, padding:"2px 4px", borderRadius:4, textAlign:"center" }}>{d.date}</div>
                <span style={{ fontSize:11, color:"#374151", flex:1 }}>{d.label}</span>
                {d.urgent&&<span style={{ fontSize:8.5, background:"#fee2e2", color:"#dc2626", padding:"1px 4px", borderRadius:3, fontWeight:700 }}>NOW</span>}
              </div>
            ))}
          </div>
        </Card>

        {/* Tasks + Group Stats */}
        <div style={{ display:"flex", flexDirection:"column", gap:14 }}>
          <Card>
            <div style={{ padding:"11px 16px", borderBottom:"1px solid #f1f5f9", display:"flex", justifyContent:"space-between" }}>
              <span style={{ fontWeight:700, fontSize:13, color:"#0f172a" }}>📋 Open Tasks</span>
              <span style={{ fontSize:11, color:"#94a3b8" }}>{TASKS.filter(t=>t.status!=="Done").length} pending</span>
            </div>
            <div style={{ padding:"6px 12px" }}>
              {TASKS.filter(t=>t.status!=="Done").slice(0,5).map((t,i)=>(
                <div key={i} style={{ padding:"6px 4px", borderBottom:"1px solid #f8fafc", display:"flex", gap:7, alignItems:"flex-start" }}>
                  <span style={{ width:6, height:6, borderRadius:"50%", background:t.status==="Overdue"?"#dc2626":"#3b82f6", display:"inline-block", marginTop:5, flexShrink:0 }} />
                  <div style={{ flex:1 }}>
                    <div style={{ fontSize:11.5, color:"#0f172a", fontWeight:500 }}>{t.task}</div>
                    <div style={{ fontSize:10, color:"#94a3b8" }}>{t.client} · {t.assigned} · {t.due}</div>
                  </div>
                  {t.status==="Overdue"&&<span style={{ fontSize:8.5, background:"#fee2e2", color:"#dc2626", padding:"1px 4px", borderRadius:3, fontWeight:700, flexShrink:0 }}>OVERDUE</span>}
                </div>
              ))}
            </div>
          </Card>

          <Card style={{ padding:"14px 16px" }}>
            <div style={{ fontWeight:700, fontSize:12, color:"#0f172a", marginBottom:10 }}>Filing by Group</div>
            {["Salary","Business","Company","HUF"].map(g=>{
              const grp = clients.filter(c=>c.group===g);
              const done = grp.filter(c=>c.itrStatus==="Filed").length;
              return (
                <div key={g} style={{ marginBottom:8 }}>
                  <div style={{ display:"flex", justifyContent:"space-between", marginBottom:3 }}>
                    <span style={{ fontSize:11, color:"#374151" }}>{g}</span>
                    <span style={{ fontSize:10.5, color:"#64748b" }}>{done}/{grp.length}</span>
                  </div>
                  <div style={{ height:5, background:"#f1f5f9", borderRadius:3 }}>
                    <div style={{ width:grp.length?`${done/grp.length*100}%`:"0%", height:"100%", background:done===grp.length?"#15803d":"#3b82f6", borderRadius:3 }} />
                  </div>
                </div>
              );
            })}
          </Card>
        </div>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════════
   CLIENT MASTER
══════════════════════════════════════════════════════════════════════════ */
function ClientsModule({ clients, onOpenITR }) {
  const [search, setSearch]   = useState("");
  const [filter, setFilter]   = useState("All");
  const [grpFilter, setGrp]   = useState("All");
  const [selIds, setSel]       = useState([]);
  const fileRef                = useRef();

  const filtered = useMemo(()=>clients.filter(c=>{
    const ms = !search || c.name.toLowerCase().includes(search.toLowerCase()) || c.pan.toLowerCase().includes(search.toLowerCase());
    const mf = filter==="All" || c.itrStatus===filter;
    const mg = grpFilter==="All" || c.group===grpFilter;
    return ms && mf && mg;
  }),[clients,search,filter,grpFilter]);

  const allSel   = filtered.length>0 && filtered.every(c=>selIds.includes(c.id));
  const toggleAll= () => allSel ? setSel([]) : setSel(filtered.map(c=>c.id));
  const toggleOne= id => setSel(p=>p.includes(id)?p.filter(x=>x!==id):[...p,id]);

  return (
    <div style={{ display:"flex", flexDirection:"column", height:"100%", overflow:"hidden" }}>
      <div style={{ padding:"10px 18px", background:"#fff", borderBottom:"1px solid #e8edf5", display:"flex", gap:10, alignItems:"center", flexShrink:0 }}>
        <div style={{ position:"relative", maxWidth:280 }}>
          <span style={{ position:"absolute", left:9, top:"50%", transform:"translateY(-50%)", fontSize:12, color:"#94a3b8", pointerEvents:"none" }}>🔍</span>
          <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search name or PAN…"
            style={{ width:260, border:"1.5px solid #e2e8f0", borderRadius:6, padding:"6px 10px 6px 28px", fontSize:12.5, outline:"none" }}
            onFocus={e=>e.target.style.borderColor="#3b82f6"} onBlur={e=>e.target.style.borderColor="#e2e8f0"} />
        </div>
        {["All","Filed","Pending","In Progress"].map(f=>(
          <button key={f} onClick={()=>setFilter(f)} style={{ background:filter===f?"#1565c0":"#f1f5f9", color:filter===f?"#fff":"#475569", border:"none", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>{f}</button>
        ))}
        <select value={grpFilter} onChange={e=>setGrp(e.target.value)} style={{ border:"1px solid #e2e8f0", borderRadius:5, padding:"5px 8px", fontSize:11.5, outline:"none", background:"#fff" }}>
          {["All","Salary","Business","Company","HUF"].map(g=><option key={g}>{g}</option>)}
        </select>
        <div style={{ flex:1 }} />
        {selIds.length>0&&(
          <div style={{ display:"flex", gap:8, alignItems:"center" }}>
            <span style={{ fontSize:11.5, color:"#1565c0", fontWeight:600 }}>{selIds.length} selected</span>
            <button style={{ background:"#1565c0", border:"none", color:"#fff", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>⚡ Bulk e-File (JSON)</button>
            <button style={{ background:"#f0fdf4", border:"1px solid #86efac", color:"#15803d", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>📄 Bulk Print</button>
          </div>
        )}
        <button onClick={()=>fileRef.current.click()} style={{ background:"#0f172a", border:"none", color:"#e2eaf6", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>⬆ Import JSON</button>
        <input ref={fileRef} type="file" accept=".json" style={{ display:"none" }} onChange={()=>{}} />
        <button style={{ background:"#f0fdf4", border:"1px solid #86efac", color:"#15803d", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>+ Add Client</button>
      </div>

      <div style={{ flex:1, overflowY:"auto" }}>
        <table style={{ width:"100%", borderCollapse:"collapse" }}>
          <thead style={{ position:"sticky", top:0, background:"#f8fafc", zIndex:2 }}>
            <tr>
              <th style={{ width:36, padding:"8px 10px" }}><input type="checkbox" checked={allSel} onChange={toggleAll} /></th>
              {["Client Name","PAN","Status","City","ITR Type","Assigned","Filing Status","TDS","Action"].map(h=>(
                <th key={h} style={{ padding:"8px 10px", textAlign:"left", fontSize:10, fontWeight:700, color:"#94a3b8", letterSpacing:"0.05em", textTransform:"uppercase", whiteSpace:"nowrap", borderBottom:"1.5px solid #e8edf5" }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.map(c=>(
              <tr key={c.id} style={{ borderBottom:"1px solid #f1f5f9" }} onMouseEnter={e=>e.currentTarget.style.background="#f8fafc"} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
                <td style={{ padding:"7px 10px" }}><input type="checkbox" checked={selIds.includes(c.id)} onChange={()=>toggleOne(c.id)} /></td>
                <td style={{ padding:"7px 10px" }}>
                  <div style={{ fontSize:12.5, fontWeight:600, color:"#0f172a" }}>{c.name}</div>
                  <div style={{ fontSize:10.5, color:"#94a3b8" }}>{c.email}</div>
                </td>
                <td style={{ padding:"7px 10px", fontFamily:"monospace", fontSize:11.5, fontWeight:600, color:"#374151" }}>{c.pan}</td>
                <td style={{ padding:"7px 10px" }}><Chip label={c.status} /></td>
                <td style={{ padding:"7px 10px", fontSize:11.5, color:"#64748b" }}>{c.city}</td>
                <td style={{ padding:"7px 10px" }}><span style={{ fontSize:10.5, fontWeight:700, color:"#1d4ed8", background:"#eff6ff", padding:"2px 6px", borderRadius:4 }}>{c.itrType}</span></td>
                <td style={{ padding:"7px 10px", fontSize:11.5, color:"#64748b" }}>{c.assignedTo}</td>
                <td style={{ padding:"7px 10px" }}><StatusBadge status={c.itrStatus} /></td>
                <td style={{ padding:"7px 10px", fontFamily:"monospace", fontSize:11.5 }}>{c.tdsAmt>0?`₹ ${c.tdsAmt.toLocaleString("en-IN")}`:"—"}</td>
                <td style={{ padding:"7px 10px" }}>
                  <button onClick={()=>onOpenITR(c)} style={{ background:"#1565c0", border:"none", color:"#fff", borderRadius:5, padding:"4px 10px", fontSize:11, cursor:"pointer", fontWeight:600 }}>Open ITR →</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════════
   ITR COMPUTATION  —  AUTO REGIME SELECTION
══════════════════════════════════════════════════════════════════════════ */
function ITRModule({ clients, initialClient }) {
  const [selClient, setSelClient] = useState(initialClient || null);
  const [search, setSearch]       = useState(initialClient?.name || "");
  const [showDrop, setShowDrop]   = useState(false);
  const [d, setD]                 = useState({});
  const [shareOpen, setShareOpen] = useState(false);
  const fileRef                   = useRef();
  const set = k => v => setD(p=>({...p,[k]:v}));

  const filtered = useMemo(()=>search.length>0
    ? clients.filter(c=>c.name.toLowerCase().includes(search.toLowerCase())||c.pan.toLowerCase().includes(search.toLowerCase()))
    : clients
  ,[clients,search]);

  const selectClient = c => { setSelClient(c); setSearch(c.name); setShowDrop(false); setD({}); };

  /* Auto regime: compute both, pick the one with lower tax */
  const oldTax = selClient ? computeTax(d, "O", selClient) : null;
  const newTax = selClient ? computeTax(d, "N", selClient) : null;
  const autoRegime  = !selClient ? null : (newTax?.grossTaxLiab <= oldTax?.grossTaxLiab ? "N" : "O");
  const C           = autoRegime === "N" ? newTax : oldTax;
  const savingAmt   = autoRegime && Math.abs((oldTax?.grossTaxLiab||0) - (newTax?.grossTaxLiab||0));

  function AmtRow({ label, k, deduction, bold, auto, total, note, indent=0, value, invisible, sub }) {
    if (invisible) return null;
    const bg = total==="grand"?"#0f172a":total==="major"?"#1e3a5f":total==="minor"?"#e8f0fe":total==="pos"?"#f0fdf4":total==="neg"?"#fef2f2":"transparent";
    const lc = total==="grand"||total==="major"?"#e2eaf6":total==="minor"?"#1e40af":total==="pos"?"#14532d":total==="neg"?"#991b1b":bold?"#0f172a":"#374151";
    const ac = deduction?"#15803d":total==="grand"||total==="major"?"#93c5fd":total==="pos"?"#15803d":total==="neg"?"#dc2626":"#0f172a";
    const dv = value != null ? value : n(d[k]);
    return (
      <tr style={{ background:bg }} onMouseEnter={e=>{ if(!total) e.currentTarget.style.background="#f8fafc"; }} onMouseLeave={e=>{ if(!total) e.currentTarget.style.background=bg||"transparent"; }}>
        <td style={{ padding:"4px 8px 4px 0", width:"52%" }}>
          <div style={{ paddingLeft:8+indent*16, display:"flex", alignItems:"center", gap:5 }}>
            {deduction&&<span style={{ fontSize:9, color:"#94a3b8" }}>−</span>}
            <span style={{ fontSize:total==="grand"||total==="major"?12:bold?12:11.5, fontWeight:total||bold?700:400, color:lc }}>{label}</span>
            {auto&&<span style={{ fontSize:8, background:"#bbf7d0", color:"#14532d", padding:"0px 4px", borderRadius:2, fontWeight:700 }}>AUTO</span>}
            {note&&<span style={{ fontSize:10, color:"#94a3b8", fontStyle:"italic" }}>({note})</span>}
          </div>
          {sub&&<div style={{ paddingLeft:8+indent*16, fontSize:10, color:"#94a3b8", marginTop:1 }}>{sub}</div>}
        </td>
        <td style={{ width:"22%", padding:"2px 6px" }}>
          {k&&!auto&&!total&&value==null&&(
            <div style={{ position:"relative" }}>
              <span style={{ position:"absolute", left:6, top:"50%", transform:"translateY(-50%)", fontSize:10, color:"#94a3b8", pointerEvents:"none" }}>₹</span>
              <input type="number" value={d[k]||""} onChange={e=>set(k)(e.target.value)}
                style={{ width:"100%", border:"1px solid #e2e8f0", borderRadius:3, padding:"3px 6px 3px 16px", fontSize:11.5, fontFamily:"monospace", textAlign:"right", background:"#fff", outline:"none", boxSizing:"border-box" }}
                onFocus={e=>e.target.style.borderColor="#3b82f6"} onBlur={e=>e.target.style.borderColor="#e2e8f0"} />
            </div>
          )}
        </td>
        <td style={{ width:"26%", padding:"3px 12px 3px 4px", textAlign:"right", fontFamily:"monospace" }}>
          {(auto||total||value!=null)&&(
            <span style={{ fontSize:total==="grand"||total==="major"?13:total?12.5:11.5, fontWeight:total||auto?700:500, color:ac }}>
              {deduction&&dv>0?"("+dv.toLocaleString("en-IN")+")":dv!==0?dv.toLocaleString("en-IN"):""}
            </span>
          )}
        </td>
      </tr>
    );
  }
  function Sep() { return <tr><td colSpan={3} style={{ padding:0, lineHeight:0 }}><div style={{ height:1, background:"#f1f5f9", margin:"2px 0" }} /></td></tr>; }

  return (
    <div style={{ display:"flex", flexDirection:"column", height:"100%", overflow:"hidden" }}>
      {/* Search bar */}
      <div style={{ background:"#0f172a", padding:"7px 14px", display:"flex", alignItems:"center", gap:10, flexShrink:0 }}>
        <div style={{ position:"relative", flex:1, maxWidth:360 }}>
          <span style={{ position:"absolute", left:9, top:"50%", transform:"translateY(-50%)", color:"#4a6a8a", fontSize:12 }}>🔍</span>
          <input value={search} onChange={e=>{ setSearch(e.target.value); setShowDrop(true); if(!e.target.value) setSelClient(null); }}
            onFocus={()=>setShowDrop(true)} onBlur={()=>setTimeout(()=>setShowDrop(false),150)}
            placeholder="Search client to open computation…"
            style={{ width:"100%", background:"#1a3550", border:"1.5px solid #1e3a5f", borderRadius:6, padding:"5px 10px 5px 28px", color:"#e2eaf6", fontSize:12.5, outline:"none" }} />
          {showDrop&&filtered.length>0&&(
            <div style={{ position:"absolute", top:"110%", left:0, right:0, background:"#0d2137", border:"1px solid #1e3a5f", borderRadius:7, zIndex:999, maxHeight:220, overflowY:"auto", boxShadow:"0 8px 24px rgba(0,0,0,0.4)" }}>
              {filtered.map((c,i)=>(
                <div key={i} onMouseDown={()=>selectClient(c)} style={{ padding:"8px 12px", cursor:"pointer", borderBottom:"1px solid #1a3550", display:"flex", justifyContent:"space-between", alignItems:"center" }}
                  onMouseEnter={e=>e.currentTarget.style.background="#1a3550"} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
                  <div>
                    <div style={{ fontSize:12.5, fontWeight:600, color:"#e2eaf6" }}>{c.name}</div>
                    <div style={{ fontSize:10.5, color:"#4a7a9a", fontFamily:"monospace" }}>{c.pan} · {c.city}</div>
                  </div>
                  <Chip label={c.status} />
                </div>
              ))}
            </div>
          )}
        </div>

        {selClient&&(
          <div style={{ display:"flex", alignItems:"center", gap:8, background:"#0a1829", border:"1.5px solid #1e3a5f", borderRadius:7, padding:"4px 12px" }}>
            <Chip label={selClient.status} />
            <div>
              <div style={{ fontSize:12.5, fontWeight:700, color:"#e2eaf6" }}>{selClient.name}</div>
              <div style={{ fontSize:10, color:"#4a7a9a", fontFamily:"monospace" }}>{selClient.pan} · Age {selClient.age||"N/A"}{C?.isSenior?" · 👴 Senior":""}</div>
            </div>
            {/* Auto regime chip */}
            {autoRegime&&(
              <div style={{ background:autoRegime==="N"?"#f0fdf4":"#fffbeb", border:`1px solid ${autoRegime==="N"?"#86efac":"#fcd34d"}`, borderRadius:5, padding:"3px 10px" }}>
                <div style={{ fontSize:8.5, fontWeight:700, color:"#6b7280", letterSpacing:"0.04em" }}>AUTO-SELECTED REGIME</div>
                <div style={{ fontSize:11, fontWeight:800, color:autoRegime==="N"?"#15803d":"#92400e" }}>
                  {autoRegime==="N"?"New (115BAC) ✓":"Old Regime ✓"}
                  {savingAmt>0&&<span style={{ fontSize:9, color:"#64748b", marginLeft:5 }}>saves ₹{savingAmt.toLocaleString("en-IN")}</span>}
                </div>
              </div>
            )}
          </div>
        )}

        <div style={{ flex:1 }} />
        {selClient&&<>
          <button onClick={()=>setShareOpen(true)} style={{ background:"#059669", border:"none", color:"#fff", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>📤 Email / WhatsApp Client</button>
          <button style={{ background:"#1565c0", border:"none", color:"#fff", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>⬇ Export JSON</button>
          <button onClick={()=>fileRef.current.click()} style={{ background:"#374151", border:"none", color:"#fff", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>⬆ Import JSON</button>
          <input ref={fileRef} type="file" accept=".json" style={{ display:"none" }} onChange={()=>{}} />
          <button style={{ background:"#14532d", border:"none", color:"#bbf7d0", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>🖨 Print</button>
        </>}
      </div>

      {/* Regime comparison strip */}
      {selClient&&C&&(
        <div style={{ background:"#f8fafc", borderBottom:"1.5px solid #e2e8f0", padding:"6px 16px", display:"flex", gap:16, alignItems:"center", flexShrink:0 }}>
          <span style={{ fontSize:11, fontWeight:700, color:"#374151" }}>⚖️ Regime Comparison:</span>
          <div style={{ display:"flex", gap:12 }}>
            {[["Old Regime",oldTax],["New (115BAC)",newTax]].map(([lbl,t])=>(
              <div key={lbl} style={{ display:"flex", gap:6, alignItems:"center", padding:"3px 10px", borderRadius:6,
                background: (lbl.startsWith("Old")&&autoRegime==="O")||(lbl.startsWith("New")&&autoRegime==="N") ? "#dcfce7" : "#fff",
                border:`1.5px solid ${(lbl.startsWith("Old")&&autoRegime==="O")||(lbl.startsWith("New")&&autoRegime==="N")?"#86efac":"#e2e8f0"}` }}>
                {((lbl.startsWith("Old")&&autoRegime==="O")||(lbl.startsWith("New")&&autoRegime==="N"))&&<span style={{ fontSize:11, color:"#15803d" }}>✓</span>}
                <span style={{ fontSize:11, color:"#374151", fontWeight:600 }}>{lbl}:</span>
                <span style={{ fontSize:11.5, fontFamily:"monospace", fontWeight:800,
                  color:(lbl.startsWith("Old")&&autoRegime==="O")||(lbl.startsWith("New")&&autoRegime==="N")?"#15803d":"#374151" }}>
                  ₹ {t?.grossTaxLiab?.toLocaleString("en-IN")||"—"}
                </span>
              </div>
            ))}
          </div>
          <span style={{ fontSize:10.5, color:"#94a3b8", fontStyle:"italic" }}>Regime auto-selected based on lower tax liability. Overridable on filing.</span>
        </div>
      )}

      {/* Col headers */}
      <div style={{ background:"#e8edf5", borderBottom:"1.5px solid #c8d4e3", padding:"3px 0", flexShrink:0, display:"flex" }}>
        <table style={{ flex:1, tableLayout:"fixed" }}><colgroup><col style={{ width:"52%" }}/><col style={{ width:"22%" }}/><col style={{ width:"26%" }}/></colgroup>
          <thead><tr>
            <th style={{ padding:"0 8px", textAlign:"left", fontSize:10, fontWeight:700, color:"#5a7a9a", letterSpacing:"0.08em", textTransform:"uppercase" }}>PARTICULARS</th>
            <th style={{ padding:"0 6px", textAlign:"right", fontSize:10, fontWeight:700, color:"#5a7a9a", letterSpacing:"0.08em", textTransform:"uppercase" }}>INPUT</th>
            <th style={{ padding:"0 12px", textAlign:"right", fontSize:10, fontWeight:700, color:"#5a7a9a", letterSpacing:"0.08em", textTransform:"uppercase" }}>AMOUNT (₹)</th>
          </tr></thead>
        </table>
        <div style={{ width:268, borderLeft:"1.5px solid #c8d4e3", padding:"0 12px", display:"flex", alignItems:"center" }}>
          <span style={{ fontSize:10, fontWeight:700, color:"#5a7a9a", letterSpacing:"0.08em", textTransform:"uppercase" }}>TAX SUMMARY</span>
        </div>
      </div>

      {/* Main */}
      <div style={{ flex:1, display:"flex", overflow:"hidden" }}>
        <div style={{ flex:1, overflowY:"auto", background:"#fff" }}>
          {!selClient?(
            <div style={{ display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", height:"100%", gap:12, color:"#94a3b8" }}>
              <div style={{ fontSize:40 }}>🧾</div>
              <div style={{ fontSize:14, fontWeight:600, color:"#64748b" }}>Search a client above to open their computation sheet</div>
              <div style={{ fontSize:12, color:"#94a3b8" }}>Regime is auto-selected based on which gives lower tax liability</div>
            </div>
          ):(
            <table style={{ tableLayout:"fixed", width:"100%" }}><colgroup><col style={{ width:"52%" }}/><col style={{ width:"22%" }}/><col style={{ width:"26%" }}/></colgroup>
            <tbody>
              <AmtRow label="A.  INCOME FROM SALARIES" bold />
              <AmtRow indent={1} label="Salary / Wages / Pension  u/s 17(1)" k="salary" />
              <AmtRow indent={1} label="Perquisites & Profits in lieu  u/s 17(2)&(3)" k="perq" />
              <AmtRow indent={1} label={`Standard Deduction  (${C.stdDedNote})`} auto deduction value={C.stdDed} invisible={C.stdDed===0} />
              <AmtRow indent={1} label="Professional Tax  u/s 16(iii)" k="profTax" deduction invisible={C.isNew} />
              <AmtRow label="Net Salary Income" bold total="minor" value={C.salaryInc>0?C.salaryInc:0} />
              <Sep/>
              <AmtRow label="B.  INCOME FROM HOUSE PROPERTY" bold />
              <AmtRow indent={1} label="Gross Annual Rent / Annual Lettable Value" k="grossRent" />
              <AmtRow indent={1} label="Municipal Taxes paid to local authority" k="muniTax" deduction invisible={!d.grossRent} />
              <AmtRow indent={1} label="Net Annual Value (NAV)" bold value={C.nav>0?C.nav:undefined} invisible={!d.grossRent} />
              <AmtRow indent={2} label="Standard Deduction  @ 30% of NAV" auto deduction value={C.hp30} invisible={C.hp30===0} />
              <AmtRow indent={2} label="Interest on Housing Loan  u/s 24(b)" k="housingInt" deduction invisible={!d.grossRent} />
              <AmtRow label="Net House Property Income" bold total={C.hpInc<0?"neg":"minor"} value={d.grossRent||d.housingInt?C.hpInc:undefined} note={C.hpInc<0?"Loss – set off u/s 71":undefined} />
              <Sep/>
              <AmtRow label="C.  CAPITAL GAINS" bold />
              <AmtRow indent={1} label="STCG – Listed Equity / EO MF  @ 15%  (Sec 111A)" k="stcg111A" />
              <AmtRow indent={1} label="STCG – Other assets / Debt MF (slab rate)" k="stcgOther" />
              <AmtRow indent={1} label="LTCG – Listed Equity / EO MF  @ 10%  (Sec 112A, ₹1L exempt)" k="ltcg112A" />
              <AmtRow indent={1} label="LTCG – Other / Immovable  @ 20% with Indexation  (Sec 112)" k="ltcg20" />
              <AmtRow indent={1} label="Less: Exemption  u/s 54 / 54EC / 54F" k="ltcgExempt" deduction invisible={!d.ltcg112A&&!d.ltcg20} />
              <AmtRow label="Net Capital Gains" bold total="minor" value={(C.totalSTCG+C.totalLTCG)>0?C.totalSTCG+C.totalLTCG:undefined} />
              <Sep/>
              <AmtRow label="D.  INCOME FROM OTHER SOURCES" bold />
              <AmtRow indent={1} label="Savings Bank Account Interest" k="sbInt" />
              <AmtRow indent={1} label="Fixed Deposit / Term Deposit / RD Interest" k="fdInt" />
              <AmtRow indent={1} label="Dividends, Family Pension, Lottery, Others" k="othSrc" />
              <AmtRow label="Net Other Sources Income" bold total="minor" value={C.otherSrc>0?C.otherSrc:undefined} />
              <Sep/>
              <AmtRow label="GROSS TOTAL INCOME  (A + B + C + D)" total="major" value={C.gti>0?C.gti:undefined} />
              <Sep/>
              {!C.isNew?(<>
                <AmtRow label="E.  CHAPTER VI-A DEDUCTIONS  (Old Regime)" bold />
                <AmtRow indent={1} label={C.isSenior?"80TTB – Interest from Banks (Senior Citizen, auto-capped ₹50,000)":"80TTA – Savings Bank Interest (auto-capped ₹10,000)"} auto deduction value={C.isSenior?C.auto80TTB:C.auto80TTA} invisible={(C.isSenior?C.auto80TTB:C.auto80TTA)===0} />
                <AmtRow indent={1} label="80C – LIC, PPF, ELSS, EPF, NSC, Home Loan Principal…  (cap ₹1,50,000)" k="sec80C" deduction sub="Enter the total of all eligible 80C investments" note={n(d.sec80C)>150000?"excess ignored":undefined} />
                <AmtRow indent={1} label="80CCD(1B) – Additional NPS Contribution  (cap ₹50,000)" k="sec80CCD1B" deduction />
                <AmtRow indent={1} label={`80D – Medical Insurance Premium  (Self/Family cap ₹${C.isSenior?"50,000":"25,000"})`} k="sec80D" deduction />
                <AmtRow indent={1} label="80E – Interest on Education Loan  (no limit)" k="sec80E" deduction />
                <AmtRow indent={1} label="80G – Donations to Eligible Institutions  (50% / 100%)" k="sec80G" deduction />
                <AmtRow label="Total Chapter VI-A Deductions" bold total="pos" value={C.totalDed>0?C.totalDed:undefined} />
                <Sep/>
              </>):(
                <AmtRow label="E.  DEDUCTIONS – New Regime (115BAC): Standard Ded. ₹75,000 only" bold total="pos" value={C.stdDed} note="80C / 80D / 80E / 80G not available" />
              )}
              <AmtRow label="TOTAL TAXABLE INCOME" total="major" value={C.taxableInc>0?C.taxableInc:undefined} />
              <Sep/>
              <AmtRow label="F.  TAX CREDITS & PAYMENTS" bold />
              <AmtRow indent={1} label="TDS as per Form 26AS / AIS / TIS" k="tds26AS" />
              <AmtRow indent={1} label="TDS deducted by Employer  (Form 16)" k="tds16" />
              <AmtRow indent={1} label="Advance Tax paid  (all four instalments)" k="advTax" />
              <AmtRow indent={1} label="Self-Assessment Tax paid  (Challan 280)" k="selfTax" />
              <AmtRow label="Total Tax Credits Paid" bold total="pos" value={C.totalPaid>0?C.totalPaid:undefined} />
              <tr><td colSpan={3} style={{ height:32 }}/></tr>
            </tbody></table>
          )}
        </div>

        {/* Summary panel */}
        <div style={{ width:268, background:"#fff", borderLeft:"2px solid #d1dde9", display:"flex", flexDirection:"column", overflowY:"auto", flexShrink:0 }}>
          {!C?(
            <div style={{ flex:1, display:"flex", alignItems:"center", justifyContent:"center", color:"#c8d4e3", flexDirection:"column", gap:8 }}>
              <div style={{ fontSize:28 }}>🧮</div><div style={{ fontSize:12 }}>Select client</div>
            </div>
          ):(
            <>
              <div style={{ background:"#0f172a", padding:"10px 14px" }}>
                <div style={{ fontSize:8.5, fontWeight:700, color:"#3a6a8a", letterSpacing:"0.08em", textTransform:"uppercase", marginBottom:8 }}>Income Breakdown</div>
                {[["Salary",C.salaryInc],["House Prop.",C.hpInc],["Capital Gains",C.totalSTCG+C.totalLTCG],["Other Sources",C.otherSrc]].map(([l,v])=>(
                  <div key={l} style={{ display:"flex", justifyContent:"space-between", padding:"2px 0", borderBottom:"1px solid #1a3550" }}>
                    <span style={{ fontSize:11, color:"#5a8aaa" }}>{l}</span>
                    <span style={{ fontSize:11, color:v<0?"#f87171":"#b0c8e0", fontFamily:"monospace" }}>{v?fmt(v):"—"}</span>
                  </div>
                ))}
                <div style={{ display:"flex", justifyContent:"space-between", padding:"5px 0 2px", borderTop:"1.5px solid #2a4565", marginTop:2 }}>
                  <span style={{ fontSize:11.5, fontWeight:700, color:"#42a5f5" }}>GTI</span>
                  <span style={{ fontSize:12, fontWeight:800, color:"#42a5f5", fontFamily:"monospace" }}>{C.gti>0?fmt(C.gti):"—"}</span>
                </div>
                <div style={{ display:"flex", justifyContent:"space-between", padding:"2px 0" }}>
                  <span style={{ fontSize:11, color:"#4caf50" }}>Deductions</span>
                  <span style={{ fontSize:11, color:"#4caf50", fontFamily:"monospace" }}>{C.totalDed>0?"("+fmt(C.totalDed)+")":"—"}</span>
                </div>
                <div style={{ display:"flex", justifyContent:"space-between", padding:"5px 0 0", borderTop:"1.5px solid #2a4565", marginTop:2 }}>
                  <span style={{ fontSize:12, fontWeight:800, color:"#fff" }}>Taxable Income</span>
                  <span style={{ fontSize:13, fontWeight:900, color:"#fff", fontFamily:"monospace" }}>{C.taxableInc>0?fmt(C.taxableInc):"—"}</span>
                </div>
              </div>

              <div style={{ padding:"10px 14px", borderBottom:"1px solid #e8edf5" }}>
                <div style={{ fontSize:8.5, fontWeight:700, color:"#94a3b8", letterSpacing:"0.06em", textTransform:"uppercase", marginBottom:7 }}>Tax Calculation</div>
                {[
                  { l:"Normal Income Tax",  v:Math.round(C.normalTax) },
                  C.stcgTax>0&&{ l:"STCG Tax @ 15%",   v:Math.round(C.stcgTax) },
                  C.ltcgTax>0&&{ l:"LTCG Tax @ 10%",   v:Math.round(C.ltcgTax) },
                  C.ltcg20Tax>0&&{ l:"LTCG Tax @ 20%", v:Math.round(C.ltcg20Tax) },
                ].filter(Boolean).map((r,i)=>(
                  <div key={i} style={{ display:"flex", justifyContent:"space-between", padding:"3px 0", borderBottom:"1px solid #f1f5f9" }}>
                    <span style={{ fontSize:11, color:"#4a5568" }}>{r.l}</span>
                    <span style={{ fontSize:11.5, fontFamily:"monospace" }}>{r.v?fmt(r.v):"—"}</span>
                  </div>
                ))}
                {C.rebate87A>0&&(
                  <div style={{ display:"flex", justifyContent:"space-between", padding:"3px 0", borderBottom:"1px solid #f1f5f9" }}>
                    <span style={{ fontSize:11, color:"#15803d", display:"flex", alignItems:"center", gap:4 }}>87A Rebate<span style={{ fontSize:7.5, background:"#bbf7d0", color:"#14532d", padding:"0px 3px", borderRadius:2, fontWeight:700 }}>AUTO</span></span>
                    <span style={{ fontSize:11.5, color:"#15803d", fontFamily:"monospace" }}>({fmt(Math.round(C.rebate87A))})</span>
                  </div>
                )}
                {C.surcharge>0&&(
                  <div style={{ display:"flex", justifyContent:"space-between", padding:"3px 0", borderBottom:"1px solid #f1f5f9" }}>
                    <span style={{ fontSize:11, color:"#4a5568" }}>Surcharge</span>
                    <span style={{ fontSize:11.5, fontFamily:"monospace" }}>{fmt(Math.round(C.surcharge))}</span>
                  </div>
                )}
                <div style={{ display:"flex", justifyContent:"space-between", padding:"3px 0", borderBottom:"1px solid #f1f5f9" }}>
                  <span style={{ fontSize:11, color:"#4a5568", display:"flex", alignItems:"center", gap:4 }}>Cess 4%<span style={{ fontSize:7.5, background:"#bbf7d0", color:"#14532d", padding:"0px 3px", borderRadius:2, fontWeight:700 }}>AUTO</span></span>
                  <span style={{ fontSize:11.5, fontFamily:"monospace" }}>{C.cess>0?fmt(Math.round(C.cess)):"—"}</span>
                </div>
                <div style={{ display:"flex", justifyContent:"space-between", padding:"6px 0 0", borderTop:"1.5px solid #e2e8f0", marginTop:3 }}>
                  <span style={{ fontSize:12, fontWeight:700, color:"#1565c0" }}>Gross Tax Liability</span>
                  <span style={{ fontSize:13, fontWeight:800, color:"#1565c0", fontFamily:"monospace" }}>{C.grossTaxLiab>0?fmt(C.grossTaxLiab):"—"}</span>
                </div>
              </div>

              <div style={{ padding:"8px 14px", borderBottom:"1px solid #e8edf5" }}>
                <div style={{ fontSize:8.5, fontWeight:700, color:"#94a3b8", letterSpacing:"0.06em", textTransform:"uppercase", marginBottom:6 }}>Tax Paid</div>
                {[["26AS / AIS / TIS TDS",n(d.tds26AS)],["Employer TDS (Form 16)",n(d.tds16)],["Advance Tax",n(d.advTax)],["Self-Assessment Tax",n(d.selfTax)]].map(([l,v])=>(
                  <div key={l} style={{ display:"flex", justifyContent:"space-between", padding:"2px 0", borderBottom:"1px solid #f8fafc" }}>
                    <span style={{ fontSize:11, color:"#64748b" }}>{l}</span>
                    <span style={{ fontSize:11, color:v>0?"#15803d":"#cbd5e1", fontFamily:"monospace" }}>{v>0?"("+fmt(v)+")":"—"}</span>
                  </div>
                ))}
              </div>

              <div style={{ padding:"12px 14px", background:C.isRefund?"#f0fdf4":C.balance>0?"#fef2f2":"#f8fafc", borderTop:`3px solid ${C.isRefund?"#16a34a":C.balance>0?"#dc2626":"#e2e8f0"}` }}>
                <div style={{ fontSize:10, fontWeight:700, letterSpacing:"0.05em", textTransform:"uppercase", color:C.isRefund?"#15803d":C.balance>0?"#991b1b":"#64748b", marginBottom:3 }}>
                  {C.isRefund?"✓ Refund Due":C.balance>0?"⚠ Tax Payable":"Nil Balance"}
                </div>
                <div style={{ fontSize:26, fontWeight:900, color:C.isRefund?"#16a34a":C.balance>0?"#dc2626":"#94a3b8", fontFamily:"monospace", lineHeight:1 }}>
                  {C.balance!==0?`₹ ${fmt(Math.abs(C.balance))}`:"₹ 0"}
                </div>
                {C.balance>0&&<div style={{ fontSize:10, color:"#ef4444", marginTop:5 }}>Pay via Challan 280. Int. u/s 234B/C may apply.</div>}
              </div>

              <div style={{ padding:"8px 14px", background:"#f0fdf4", borderTop:"1px solid #bbf7d0" }}>
                <div style={{ fontSize:8.5, fontWeight:700, color:"#14532d", letterSpacing:"0.06em", textTransform:"uppercase", marginBottom:5 }}>🤖 Auto-Applied</div>
                {[
                  { l:"Std. Deduction",             v:C.stdDed,      show:C.stdDed>0 },
                  { l:"HP 30% Deduction",            v:C.hp30,        show:C.hp30>0 },
                  { l:C.isSenior?"80TTB":"80TTA",   v:C.isSenior?C.auto80TTB:C.auto80TTA, show:(C.auto80TTA>0||C.auto80TTB>0) },
                  { l:"87A Rebate",                  v:C.rebate87A,   show:C.rebate87A>0 },
                  { l:"4% Health & Ed. Cess",        v:Math.round(C.cess), show:C.cess>0 },
                  { l:`Regime: ${autoRegime==="N"?"New saves":"Old saves"} ₹${fmt(savingAmt)||"0"}`, v:null, show:!!savingAmt&&savingAmt>0 },
                ].filter(r=>r.show).map((r,i)=>(
                  <div key={i} style={{ display:"flex", justifyContent:"space-between", padding:"2px 0", borderBottom:"1px solid #dcfce7" }}>
                    <span style={{ fontSize:10.5, color:"#15803d" }}>{r.l}</span>
                    {r.v!=null&&<span style={{ fontSize:10.5, color:"#15803d", fontFamily:"monospace", fontWeight:600 }}>{fmt(r.v)}</span>}
                  </div>
                ))}
              </div>

              <div style={{ padding:"10px 14px" }}>
                <button onClick={()=>setShareOpen(true)} style={{ width:"100%", background:"#059669", border:"none", color:"#fff", borderRadius:7, padding:"9px", cursor:"pointer", fontSize:12.5, fontWeight:700, marginBottom:6 }}>
                  📤 Email / WhatsApp Client
                </button>
                <button style={{ width:"100%", background:"#1565c0", border:"none", color:"#fff", borderRadius:7, padding:"8px", cursor:"pointer", fontSize:12, fontWeight:600, marginBottom:6 }}>
                  ⬇ Export Computation JSON
                </button>
                <button style={{ width:"100%", background:"#f1f5f9", border:"1px solid #e2e8f0", color:"#374151", borderRadius:7, padding:"7px", cursor:"pointer", fontSize:12, fontWeight:600 }}>
                  🖨 Print Computation Sheet
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      {shareOpen&&<ShareModal client={selClient} tax={C} onClose={()=>setShareOpen(false)} />}
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════════
   TDS MODULE
══════════════════════════════════════════════════════════════════════════ */
function TDSModule({ clients }) {
  const rows = clients.map(c=>({
    ...c,
    tds16:Math.round(c.tdsAmt*0.85), tds26:Math.round(c.tdsAmt*0.15),
    q1:Math.round(c.tdsAmt*0.25),    q2:Math.round(c.tdsAmt*0.25),
    q3:Math.round(c.tdsAmt*0.25),    q4:Math.round(c.tdsAmt*0.25),
    match:Math.random()>0.25?"Match":"Mismatch"
  }));
  return (
    <div style={{ padding:"18px 22px", overflowY:"auto", height:"100%" }}>
      <div style={{ display:"flex", gap:12, marginBottom:18 }}>
        <Metric label="Total TDS Deducted"  value={fmtCr(clients.reduce((s,c)=>s+c.tdsAmt,0))} icon="📑" />
        <Metric label="Form 16 Issued"      value={clients.filter(c=>c.tdsAmt>0).length} sub="Salary clients"     color="#059669" icon="📄" />
        <Metric label="26AS Mismatches"     value={rows.filter(r=>r.match==="Mismatch").length} sub="Need reconciliation" color="#dc2626" icon="⚠" />
        <Metric label="Advance Tax Paid"    value={fmtCr(clients.reduce((s,c)=>s+c.advance,0))} color="#7c3aed" icon="💳" />
      </div>
      <Card>
        <div style={{ padding:"11px 16px", borderBottom:"1px solid #f1f5f9", display:"flex", justifyContent:"space-between", alignItems:"center" }}>
          <span style={{ fontWeight:700, fontSize:13 }}>TDS Summary — AY 2024-25</span>
          <div style={{ display:"flex", gap:8 }}>
            <button style={{ background:"#1565c0", border:"none", color:"#fff", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>⬇ Import 26AS JSON (Bulk)</button>
            <button style={{ background:"#f0fdf4", border:"1px solid #86efac", color:"#15803d", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>📊 Reconcile All</button>
          </div>
        </div>
        <table style={{ width:"100%", borderCollapse:"collapse" }}>
          <thead style={{ background:"#f8fafc" }}>
            <tr>{["Client","PAN","Form 16 TDS","26AS TDS","Q1","Q2","Q3","Q4","Total","Status"].map(h=>(
              <th key={h} style={{ padding:"7px 10px", textAlign:"left", fontSize:10, fontWeight:700, color:"#94a3b8", letterSpacing:"0.05em", textTransform:"uppercase", borderBottom:"1.5px solid #e8edf5", whiteSpace:"nowrap" }}>{h}</th>
            ))}</tr>
          </thead>
          <tbody>
            {rows.filter(c=>c.tdsAmt>0).map(c=>(
              <tr key={c.id} style={{ borderBottom:"1px solid #f1f5f9" }} onMouseEnter={e=>e.currentTarget.style.background="#f8fafc"} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
                <td style={{ padding:"6px 10px", fontSize:12, fontWeight:600 }}>{c.name.split(" ").slice(0,2).join(" ")}</td>
                <td style={{ padding:"6px 10px", fontFamily:"monospace", fontSize:11, color:"#64748b" }}>{c.pan}</td>
                {[c.tds16,c.tds26,c.q1,c.q2,c.q3,c.q4,c.tdsAmt].map((v,i)=>(
                  <td key={i} style={{ padding:"6px 10px", fontFamily:"monospace", fontSize:11.5, textAlign:"right" }}>₹ {v.toLocaleString("en-IN")}</td>
                ))}
                <td style={{ padding:"6px 10px" }}>
                  <span style={{ fontSize:10.5, fontWeight:700, background:c.match==="Match"?"#dcfce7":"#fee2e2", color:c.match==="Match"?"#15803d":"#dc2626", padding:"2px 8px", borderRadius:20 }}>{c.match}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════════
   OFFICE MODULE
══════════════════════════════════════════════════════════════════════════ */
function OfficeModule() {
  return (
    <div style={{ padding:"18px 22px", overflowY:"auto", height:"100%" }}>
      <div style={{ display:"flex", gap:12, marginBottom:18 }}>
        <Metric label="Open Tasks"  value={TASKS.filter(t=>t.status!=="Done").length} color="#d97706" icon="📋" />
        <Metric label="Overdue"     value={TASKS.filter(t=>t.status==="Overdue").length} color="#dc2626" icon="🔴" />
        <Metric label="Done / Month" value={TASKS.filter(t=>t.status==="Done").length} color="#059669" icon="✅" />
        <Metric label="Staff"       value="4" sub="Active team members" icon="👥" />
      </div>
      <div style={{ display:"grid", gridTemplateColumns:"1fr 1fr", gap:14 }}>
        <Card>
          <div style={{ padding:"11px 16px", borderBottom:"1px solid #f1f5f9", display:"flex", justifyContent:"space-between" }}>
            <span style={{ fontWeight:700, fontSize:13 }}>Task Tracker</span>
            <button style={{ background:"#1565c0", border:"none", color:"#fff", borderRadius:5, padding:"4px 10px", fontSize:11, cursor:"pointer", fontWeight:600 }}>+ New Task</button>
          </div>
          {TASKS.map((t,i)=>(
            <div key={i} style={{ padding:"9px 14px", borderBottom:"1px solid #f8fafc", display:"flex", gap:8, alignItems:"flex-start" }}>
              <span style={{ width:7, height:7, borderRadius:"50%", background:t.status==="Done"?"#16a34a":t.status==="Overdue"?"#dc2626":"#3b82f6", display:"inline-block", marginTop:4, flexShrink:0 }} />
              <div style={{ flex:1 }}>
                <div style={{ fontSize:12.5, fontWeight:600, color:"#0f172a" }}>{t.task}</div>
                <div style={{ fontSize:11, color:"#94a3b8", marginTop:1 }}>{t.client} · <b style={{ color:"#475569" }}>{t.assigned}</b> · Due {t.due}</div>
              </div>
              <div style={{ display:"flex", flexDirection:"column", alignItems:"flex-end", gap:3 }}>
                <span style={{ fontSize:9, fontWeight:700, background:t.priority==="High"?"#fee2e2":"#fffbeb", color:t.priority==="High"?"#dc2626":"#d97706", padding:"1px 5px", borderRadius:3 }}>{t.priority}</span>
                <StatusBadge status={t.status==="Done"?"Filed":t.status==="Overdue"?"Overdue":"Pending"} />
              </div>
            </div>
          ))}
        </Card>
        <Card>
          <div style={{ padding:"11px 16px", borderBottom:"1px solid #f1f5f9" }}><span style={{ fontWeight:700, fontSize:13 }}>Staff Workload</span></div>
          <div style={{ padding:"14px 16px" }}>
            {["Priya","Amit","Rajan"].map(staff=>{
              const mine = TASKS.filter(t=>t.assigned===staff);
              const done = mine.filter(t=>t.status==="Done").length;
              return (
                <div key={staff} style={{ marginBottom:14 }}>
                  <div style={{ display:"flex", justifyContent:"space-between", marginBottom:4 }}>
                    <span style={{ fontSize:13, fontWeight:600 }}>{staff}</span>
                    <span style={{ fontSize:11, color:"#64748b" }}>{done}/{mine.length} tasks complete</span>
                  </div>
                  <div style={{ height:7, background:"#f1f5f9", borderRadius:4, marginBottom:3 }}>
                    <div style={{ width:mine.length?`${done/mine.length*100}%`:"0%", height:"100%", background:done===mine.length?"#15803d":"#3b82f6", borderRadius:4 }} />
                  </div>
                  <div style={{ fontSize:10.5, color:"#94a3b8" }}>{mine.filter(t=>t.status==="Overdue").length} overdue · {mine.filter(t=>t.status==="Open").length} open</div>
                </div>
              );
            })}
          </div>
        </Card>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════════
   BILLING MODULE
══════════════════════════════════════════════════════════════════════════ */
function BillingModule({ clients }) {
  const inv = clients.map((c,i)=>{
    const fees = c.itrType==="ITR-1"?2500:c.itrType==="ITR-2"?5000:c.itrType==="ITR-3"?15000:c.itrType==="ITR-5"?20000:25000;
    const gst  = Math.round(fees*0.18);
    return { ...c, invNo:`INV-2024-${String(i+1).padStart(3,"0")}`, fees, gst, total:fees+gst, paid:c.itrStatus==="Filed" };
  });
  const totBilled   = inv.reduce((s,i)=>s+i.total,0);
  const totReceived = inv.filter(i=>i.paid).reduce((s,i)=>s+i.total,0);
  return (
    <div style={{ padding:"18px 22px", overflowY:"auto", height:"100%" }}>
      <div style={{ display:"flex", gap:12, marginBottom:18 }}>
        <Metric label="Total Billed"       value={fmtCr(totBilled)}            icon="💼" />
        <Metric label="Received"           value={fmtCr(totReceived)}           color="#059669" icon="✅" />
        <Metric label="Pending Collection" value={fmtCr(totBilled-totReceived)} color="#dc2626" icon="⏳" />
        <Metric label="GST on Fees"        value={fmtCr(inv.reduce((s,i)=>s+i.gst,0))} color="#7c3aed" icon="🧾" />
      </div>
      <Card>
        <div style={{ padding:"11px 16px", borderBottom:"1px solid #f1f5f9", display:"flex", justifyContent:"space-between" }}>
          <span style={{ fontWeight:700, fontSize:13 }}>Fee Invoices — AY 2024-25</span>
          <div style={{ display:"flex", gap:8 }}>
            <button style={{ background:"#1565c0", border:"none", color:"#fff", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>+ Generate All Invoices</button>
            <button style={{ background:"#f0fdf4", border:"1px solid #86efac", color:"#15803d", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>📧 Email All Invoices</button>
            <button style={{ background:"#fef9c3", border:"1px solid #fcd34d", color:"#92400e", borderRadius:5, padding:"5px 12px", fontSize:11.5, cursor:"pointer", fontWeight:600 }}>💬 WhatsApp Reminders</button>
          </div>
        </div>
        <table style={{ width:"100%", borderCollapse:"collapse" }}>
          <thead style={{ background:"#f8fafc" }}>
            <tr>{["Invoice No.","Client","ITR Type","Fees","GST @18%","Total","Status","Action"].map(h=>(
              <th key={h} style={{ padding:"7px 10px", textAlign:"left", fontSize:10, fontWeight:700, color:"#94a3b8", letterSpacing:"0.05em", textTransform:"uppercase", borderBottom:"1.5px solid #e8edf5", whiteSpace:"nowrap" }}>{h}</th>
            ))}</tr>
          </thead>
          <tbody>
            {inv.map(i=>(
              <tr key={i.id} style={{ borderBottom:"1px solid #f1f5f9" }} onMouseEnter={e=>e.currentTarget.style.background="#f8fafc"} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
                <td style={{ padding:"6px 10px", fontFamily:"monospace", fontSize:11, color:"#1d4ed8", fontWeight:600 }}>{i.invNo}</td>
                <td style={{ padding:"6px 10px", fontSize:12, fontWeight:600 }}>{i.name.split(" ").slice(0,2).join(" ")}</td>
                <td style={{ padding:"6px 10px" }}><span style={{ fontSize:10.5, fontWeight:700, color:"#1d4ed8", background:"#eff6ff", padding:"2px 6px", borderRadius:4 }}>{i.itrType}</span></td>
                <td style={{ padding:"6px 10px", fontFamily:"monospace", fontSize:11.5, textAlign:"right" }}>₹ {i.fees.toLocaleString("en-IN")}</td>
                <td style={{ padding:"6px 10px", fontFamily:"monospace", fontSize:11.5, textAlign:"right", color:"#7c3aed" }}>₹ {i.gst.toLocaleString("en-IN")}</td>
                <td style={{ padding:"6px 10px", fontFamily:"monospace", fontSize:12, fontWeight:700, textAlign:"right" }}>₹ {i.total.toLocaleString("en-IN")}</td>
                <td style={{ padding:"6px 10px" }}><StatusBadge status={i.paid?"Filed":"Pending"} /></td>
                <td style={{ padding:"6px 10px", display:"flex", gap:5 }}>
                  <button style={{ background:"#f1f5f9", border:"none", color:"#374151", borderRadius:4, padding:"3px 7px", fontSize:10.5, cursor:"pointer" }}>📄 PDF</button>
                  <button style={{ background:"#f0fdf4", border:"none", color:"#15803d", borderRadius:4, padding:"3px 7px", fontSize:10.5, cursor:"pointer" }}>📧</button>
                  <button style={{ background:"#fef9c3", border:"none", color:"#92400e", borderRadius:4, padding:"3px 7px", fontSize:10.5, cursor:"pointer" }}>💬</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════════
   REPORTS MODULE
══════════════════════════════════════════════════════════════════════════ */
function ReportsModule({ clients }) {
  const rpts = [
    { label:"Filing Status Report",       desc:"Client-wise ITR filing status with due dates & remarks",        icon:"📊", color:"#1565c0" },
    { label:"TDS Reconciliation",         desc:"26AS vs Form 16 mismatch analysis client-wise",                 icon:"📑", color:"#059669" },
    { label:"Advance Tax Register",       desc:"Quarter-wise advance tax obligations & shortfall",              icon:"💳", color:"#d97706" },
    { label:"Regime Comparison Report",   desc:"Old vs New regime tax saving per client — auto-recommendation", icon:"⚖️", color:"#7c3aed" },
    { label:"Deduction Optimizer",        desc:"Suggested investments to maximise deductions per client",       icon:"🤖", color:"#0891b2" },
    { label:"Outstanding Fees Report",    desc:"Client-wise pending invoice & collection summary",              icon:"💼", color:"#dc2626" },
    { label:"Capital Gains Summary",      desc:"Scrip-wise STCG / LTCG consolidated across clients",           icon:"📈", color:"#7c3aed" },
    { label:"Computation Sheets (Bulk)",  desc:"Print / export all client computation sheets as JSON / PDF",   icon:"🗂️", color:"#1565c0" },
    { label:"Form 26AS Bulk Download",    desc:"One-click 26AS JSON fetch & import for all clients",           icon:"⬇",  color:"#059669" },
  ];
  return (
    <div style={{ padding:"18px 22px", overflowY:"auto", height:"100%" }}>
      <div style={{ display:"grid", gridTemplateColumns:"repeat(3,1fr)", gap:12 }}>
        {rpts.map(r=>(
          <Card key={r.label} style={{ padding:"14px 16px", cursor:"pointer" }}>
            <div style={{ display:"flex", alignItems:"center", gap:10, marginBottom:8 }}>
              <div style={{ width:36, height:36, borderRadius:8, background:`${r.color}18`, display:"flex", alignItems:"center", justifyContent:"center", fontSize:18 }}>{r.icon}</div>
              <span style={{ fontSize:12.5, fontWeight:700, color:"#0f172a" }}>{r.label}</span>
            </div>
            <div style={{ fontSize:11.5, color:"#64748b", marginBottom:10, lineHeight:1.5 }}>{r.desc}</div>
            <button style={{ background:r.color, border:"none", color:"#fff", borderRadius:5, padding:"5px 12px", fontSize:11, cursor:"pointer", fontWeight:600, width:"100%" }}>Generate →</button>
          </Card>
        ))}
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════════
   ROOT APP WITH MENU BAR
══════════════════════════════════════════════════════════════════════════ */
const NAV = [
  { id:"dashboard", label:"Dashboard",      icon:"📊" },
  { id:"clients",   label:"Client Master",  icon:"👥" },
  { id:"itr",       label:"ITR Computation",icon:"🧾" },
  { id:"tds",       label:"TDS / 26AS",     icon:"📑" },
  { id:"office",    label:"Office Mgmt",    icon:"📋" },
  { id:"billing",   label:"Billing",        icon:"💼" },
  { id:"reports",   label:"Reports",        icon:"📈" },
];

const MENU_BAR = [
  { label:"File", items:[
    { label:"⬆ Import Client JSON (Bulk)",   action:"import_json"  },
    { label:"⬇ Export Client Data (JSON)",   action:"export_json"  },
    { label:"📄 Export Computation Sheet",   action:"export_sheet" },
    { label:"🖨 Print Computation",          action:"print"        },
    { sep:true },
    { label:"💾 Save Current Client",        action:"save"         },
    { label:"🗂 Save All Clients",            action:"save_all"     },
    { sep:true },
    { label:"✕ Exit",                        action:"exit"         },
  ]},
  { label:"View", items:[
    { label:"📊 Dashboard",          action:"nav_dashboard" },
    { label:"👥 Client Master",      action:"nav_clients"   },
    { label:"🧾 ITR Computation",    action:"nav_itr"       },
    { label:"📑 TDS / 26AS",         action:"nav_tds"       },
    { label:"📈 Reports",            action:"nav_reports"   },
    { sep:true },
    { label:"🌙 Toggle Sidebar",     action:"toggle_sidebar"},
  ]},
  { label:"Forms", items:[
    { label:"📋 Form 16 Viewer",          action:"form16"    },
    { label:"📋 Form 26AS Import (JSON)", action:"form26AS"  },
    { label:"📋 Form 10IE – New Regime",  action:"form10IE"  },
    { label:"📋 Form 12BA – Perquisites", action:"form12BA"  },
    { label:"📋 Challan 280 – Self Tax",  action:"challan280"},
    { label:"📋 Form 3CA / 3CD – Audit", action:"form3CA"   },
    { sep:true },
    { label:"📋 Schedule AL – Assets",    action:"schedAL"   },
    { label:"📋 Schedule CFL – Loss C/F", action:"schedCFL"  },
    { label:"📋 Schedule 112A – LTCG",    action:"sched112A" },
  ]},
  { label:"Online", items:[
    { label:"🌐 e-File on Income Tax Portal",      action:"efile"      },
    { label:"⬇ Fetch 26AS / AIS / TIS (JSON)",    action:"fetch26AS"  },
    { label:"🔁 Bulk 26AS Download – All Clients", action:"bulk26AS"   },
    { label:"✅ Validate JSON before Filing",       action:"validate"   },
    { label:"⚡ Generate e-Return JSON",            action:"genJSON"    },
    { sep:true },
    { label:"📤 Email Computation to Client",      action:"emailClient"},
    { label:"💬 WhatsApp Computation to Client",   action:"waClient"   },
    { label:"📧 Bulk Email All Clients",           action:"bulkEmail"  },
  ]},
  { label:"Reports", items:[
    { label:"📊 Filing Status Report",     action:"rep_filing"   },
    { label:"📑 TDS Reconciliation",       action:"rep_tds"      },
    { label:"⚖️ Regime Comparison Report", action:"rep_regime"   },
    { label:"💳 Advance Tax Register",     action:"rep_advtax"   },
    { label:"📈 Capital Gains Summary",    action:"rep_cg"       },
    { label:"💼 Outstanding Fees",         action:"rep_billing"  },
    { sep:true },
    { label:"🗂 Bulk Print All Clients",   action:"bulk_print"   },
    { label:"🗂 Export All JSON",          action:"bulk_json"    },
  ]},
  { label:"Help", items:[
    { label:"📖 User Manual",           action:"manual"  },
    { label:"🎓 ITR Filing Guide",      action:"guide"   },
    { label:"📅 Compliance Calendar",   action:"calendar"},
    { sep:true },
    { label:"ℹ️ About MyTaxERP",        action:"about"   },
  ]},
];

export default function App() {
  const [page, setPage]       = useState("dashboard");
  const [clients]             = useState(MOCK_CLIENTS);
  const [collapsed, setCol]   = useState(false);
  const [openMenu, setMenu]   = useState(null);
  const [itrClient, setITRC]  = useState(null);
  const fileRef               = useRef();

  const openITR = useCallback(c => { setITRC(c); setPage("itr"); }, []);

  const handleMenuAction = action => {
    setMenu(null);
    if (action.startsWith("nav_")) setPage(action.replace("nav_",""));
    if (action === "import_json") fileRef.current.click();
    if (action === "toggle_sidebar") setCol(p=>!p);
  };

  return (
    <div style={{ height:"100vh", display:"flex", flexDirection:"column", fontFamily:"'Segoe UI',system-ui,sans-serif", background:"#f0f4f8", overflow:"hidden" }}>
      <style>{`*{box-sizing:border-box;margin:0;padding:0}::-webkit-scrollbar{width:5px;height:5px}::-webkit-scrollbar-thumb{background:#c1cdd8;border-radius:3px}input[type=number]::-webkit-inner-spin-button{-webkit-appearance:none}table{border-collapse:collapse;width:100%}td,th{vertical-align:middle}`}</style>

      {/* ── MENU BAR ───────────────────────────────────────────────────────── */}
      <div style={{ height:38, background:"#0d2137", display:"flex", alignItems:"stretch", flexShrink:0, borderBottom:"1px solid #1a3550", position:"relative", zIndex:200 }}>
        {/* Logo */}
        <div style={{ display:"flex", alignItems:"center", gap:7, padding:"0 14px 0 10px", borderRight:"1px solid #1a3550" }}>
          <div style={{ width:24, height:24, borderRadius:6, background:"linear-gradient(135deg,#1976d2,#42a5f5)", display:"flex", alignItems:"center", justifyContent:"center", fontSize:13, fontWeight:900, color:"#fff" }}>₹</div>
          <div>
            <div style={{ fontSize:13, fontWeight:800, color:"#e2eaf6", lineHeight:1 }}>MyTaxERP</div>
            <div style={{ fontSize:8.5, color:"#3a6a8a", letterSpacing:"0.05em" }}>INCOME TAX ERP · AY 2024-25</div>
          </div>
        </div>

        {/* Menu items */}
        {MENU_BAR.map(menu=>(
          <div key={menu.label} style={{ position:"relative" }}>
            <button
              onClick={()=>setMenu(openMenu===menu.label?null:menu.label)}
              onBlur={()=>setTimeout(()=>setMenu(null),180)}
              style={{ height:"100%", background:openMenu===menu.label?"#1e3a5f":"none", border:"none", color:"#b0c4de", cursor:"pointer", padding:"0 12px", fontSize:12.5, fontWeight:openMenu===menu.label?700:400, letterSpacing:"0.01em" }}>
              {menu.label}
            </button>
            {openMenu===menu.label&&(
              <div style={{ position:"absolute", top:"100%", left:0, minWidth:230, background:"#fff", border:"1px solid #e2e8f0", borderRadius:6, boxShadow:"0 8px 24px rgba(0,0,0,0.15)", zIndex:999 }}>
                {menu.items.map((item,i)=>
                  item.sep ? (
                    <div key={i} style={{ height:1, background:"#f1f5f9", margin:"3px 0" }} />
                  ) : (
                    <button key={i} onMouseDown={()=>handleMenuAction(item.action)}
                      style={{ width:"100%", background:"none", border:"none", padding:"7px 14px", fontSize:12.5, textAlign:"left", cursor:"pointer", color:"#374151", display:"block" }}
                      onMouseEnter={e=>e.currentTarget.style.background="#f0f4f8"} onMouseLeave={e=>e.currentTarget.style.background="none"}>
                      {item.label}
                    </button>
                  )
                )}
              </div>
            )}
          </div>
        ))}

        {/* Client count + user */}
        <div style={{ marginLeft:"auto", display:"flex", alignItems:"center", gap:10, paddingRight:14 }}>
          <span style={{ fontSize:11, color:"#3a6a8a", borderRight:"1px solid #1e3a5f", paddingRight:10 }}>
            {clients.length} Clients · {clients.filter(c=>c.itrStatus==="Filed").length} Filed
          </span>
          <div style={{ display:"flex", alignItems:"center", gap:7, background:"#0a1829", border:"1px solid #1e3a5f", borderRadius:6, padding:"4px 10px" }}>
            <div style={{ width:22, height:22, borderRadius:"50%", background:"#1565c0", display:"flex", alignItems:"center", justifyContent:"center", fontSize:10, fontWeight:700, color:"#fff" }}>CA</div>
            <div>
              <div style={{ fontSize:11.5, fontWeight:700, color:"#e2eaf6" }}>CA Firm Admin</div>
              <div style={{ fontSize:9, color:"#3a6a8a" }}>AY 2024-25 · PreFill v6.5</div>
            </div>
          </div>
        </div>
      </div>

      <input ref={fileRef} type="file" accept=".json" style={{ display:"none" }} onChange={()=>{}} />

      {/* ── BODY ─────────────────────────────────────────────────────────── */}
      <div style={{ flex:1, display:"flex", overflow:"hidden" }}>
        {/* Sidebar */}
        <div style={{ width:collapsed?46:178, background:"#0d2137", display:"flex", flexDirection:"column", transition:"width 0.2s", overflow:"hidden", flexShrink:0, borderRight:"1px solid #1a3550" }}>
          <div style={{ padding:"8px 6px", flex:1 }}>
            {NAV.map(item=>(
              <button key={item.id} onClick={()=>setPage(item.id)} title={collapsed?item.label:""}
                style={{ width:"100%", display:"flex", alignItems:"center", gap:8, padding:collapsed?"9px 0":"8px 10px", justifyContent:collapsed?"center":"flex-start", background:page===item.id?"#1e40af":"transparent", border:"none", borderRadius:6, cursor:"pointer", color:page===item.id?"#fff":"#7a9ab8", marginBottom:2, transition:"all 0.1s" }}
                onMouseEnter={e=>{ if(page!==item.id) e.currentTarget.style.background="#1a3550"; }} onMouseLeave={e=>{ if(page!==item.id) e.currentTarget.style.background="transparent"; }}>
                <span style={{ fontSize:15, flexShrink:0 }}>{item.icon}</span>
                {!collapsed&&<span style={{ fontSize:12, fontWeight:page===item.id?700:400, whiteSpace:"nowrap" }}>{item.label}</span>}
              </button>
            ))}
          </div>
          <div style={{ padding:"8px 6px", borderTop:"1px solid #1a3550" }}>
            {!collapsed&&(
              <div style={{ padding:"6px 10px", marginBottom:6 }}>
                <div style={{ fontSize:9, color:"#3a6a8a", marginBottom:3 }}>FILING PROGRESS</div>
                <div style={{ height:4, background:"#0a1829", borderRadius:2, marginBottom:3 }}>
                  <div style={{ width:`${clients.filter(c=>c.itrStatus==="Filed").length/clients.length*100}%`, height:"100%", background:"#42a5f5", borderRadius:2 }} />
                </div>
                <div style={{ fontSize:9.5, color:"#4a7a9a" }}>{clients.filter(c=>c.itrStatus==="Filed").length}/{clients.length} filed</div>
              </div>
            )}
            <button onClick={()=>setCol(p=>!p)} style={{ width:"100%", background:"none", border:"none", color:"#3a6a8a", cursor:"pointer", fontSize:18, padding:"3px 0", textAlign:"center" }}>
              {collapsed?"›":"‹"}
            </button>
          </div>
        </div>

        {/* Page Content */}
        <div style={{ flex:1, overflow:"hidden", display:"flex", flexDirection:"column" }}>
          <div style={{ background:"#fff", borderBottom:"1px solid #e8edf5", padding:"5px 18px", flexShrink:0, display:"flex", alignItems:"center", justifyContent:"space-between" }}>
            <div style={{ display:"flex", alignItems:"center", gap:6 }}>
              <span style={{ fontSize:10.5, color:"#94a3b8" }}>MyTaxERP</span>
              <span style={{ fontSize:10.5, color:"#94a3b8" }}>›</span>
              <span style={{ fontSize:12.5, fontWeight:700, color:"#0f172a" }}>{NAV.find(n=>n.id===page)?.icon} {NAV.find(n=>n.id===page)?.label}</span>
            </div>
            <div style={{ display:"flex", gap:8 }}>
              {clients.filter(c=>c.itrStatus==="Overdue").length>0&&(
                <span style={{ fontSize:11, background:"#fee2e2", color:"#dc2626", padding:"3px 10px", borderRadius:20, fontWeight:600 }}>
                  ⚠ {clients.filter(c=>c.itrStatus==="Overdue").length} overdue returns
                </span>
              )}
              <span style={{ fontSize:11, background:"#fef9c3", color:"#92400e", padding:"3px 10px", borderRadius:20, fontWeight:600 }}>⏰ ITR Due: Jul 31, 2024</span>
            </div>
          </div>

          <div style={{ flex:1, overflow:"hidden" }}>
            {page==="dashboard"&&<DashboardModule clients={clients} onNav={setPage} />}
            {page==="clients"  &&<ClientsModule   clients={clients} onOpenITR={openITR} />}
            {page==="itr"      &&<ITRModule        clients={clients} initialClient={itrClient} />}
            {page==="tds"      &&<TDSModule        clients={clients} />}
            {page==="office"   &&<OfficeModule />}
            {page==="billing"  &&<BillingModule    clients={clients} />}
            {page==="reports"  &&<ReportsModule    clients={clients} />}
          </div>
        </div>
      </div>

      {/* Status bar */}
      <div style={{ height:20, background:"#0d2137", display:"flex", alignItems:"center", padding:"0 14px", gap:0, flexShrink:0 }}>
        {[`${clients.length} Clients`,`${clients.filter(c=>c.itrStatus==="Filed").length} Filed`,`${clients.filter(c=>c.itrStatus==="Pending").length} Pending`,`${TASKS.filter(t=>t.status==="Open"||t.status==="Overdue").length} Open Tasks`,`PreFill Schema v6.5`].map((item,i,arr)=>(
          <span key={i} style={{ fontSize:10, color:"#3a6a8a", paddingRight:12, borderRight:i<arr.length-1?"1px solid #1a3550":"none", marginRight:12, whiteSpace:"nowrap" }}>{item}</span>
        ))}
        <div style={{ flex:1 }} />
        <span style={{ fontSize:10, color:"#2a4565" }}>{new Date().toLocaleDateString("en-IN",{day:"2-digit",month:"short",year:"numeric"})}</span>
      </div>
    </div>
  );
}
