const functions = require("firebase-functions");
const nodemailer = require("nodemailer");

// Gmail credentials (use Firebase environment config for production)
const gmailEmail = "michkighe@gmail.com"; // Replace with your Gmail
const gmailPassword = "qbfl twys ixuc ajwf"; // Replace with your App Password

// Create a transporter
const mailTransport = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: gmailEmail,
    pass: gmailPassword,
  },
});

// Firestore-triggered function
exports.sendEmailOnMessage = functions.firestore
  .document("messages/{messageId}")
  .onCreate(async (snap, _context) => {
    const messageData = snap.data();

    if (!messageData) {
      console.warn("No message data found");
      return null;
    }

    const mailOptions = {
      from: gmailEmail, 
      to: gmailEmail,  
      subject: `New message from ${messageData.name || "Unknown"}`,
      text: `Name: ${messageData.name || "N/A"}\nEmail: ${messageData.email || "N/A"}\n\nMessage:\n${messageData.message || ""}`,
    };

    try {
      await mailTransport.sendMail(mailOptions);
      console.log("Email sent successfully to", gmailEmail);
    } catch (error) {
      console.error("Error sending email:", error);
    }

    return null;
  });
