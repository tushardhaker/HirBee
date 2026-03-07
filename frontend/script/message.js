const myEmail = localStorage.getItem('userEmail');
const myName = localStorage.getItem('fullName') || "User";
const params = new URLSearchParams(window.location.search);
let partnerEmail = params.get('with');
let zpInstance = null; 

document.addEventListener('DOMContentLoaded', () => {
    if (!myEmail) { window.location.href = "login.html"; return; }
    loadChatPartners();
    if (partnerEmail) initChatWithPartner(partnerEmail);
    setInterval(() => { if (partnerEmail) loadMessages(); }, 3000);
});

async function loadChatPartners() {
    try {
        const res = await fetch(`https://hirbee-1.onrender.com/api/messages/partners?email=${myEmail}`);
        const partners = await res.json();
        const list = document.getElementById('contactList');
        list.innerHTML = partners.map(p => `
            <div class="contact-item p-3 d-flex align-items-center ${p === partnerEmail ? 'active' : ''}" 
                 onclick="initChatWithPartner('${p}')">
                <div class="avatar-sm bg-secondary text-white me-3">${p[0].toUpperCase()}</div>
                <div class="flex-grow-1 text-truncate">${p}</div>
            </div>
        `).join('');
    } catch (err) { console.error(err); }
}

async function initChatWithPartner(email) {
    partnerEmail = email;
    document.getElementById('partnerDisplay').innerText = email;
    document.getElementById('partnerAvatar').innerText = email.charAt(0).toUpperCase();
    document.getElementById('actionButtons').style.display = 'block';
    loadMessages();
}

async function loadMessages() {
    if (!partnerEmail) return;
    try {
        const res = await fetch(`https://hirbee-1.onrender.com/api/messages/history?me=${myEmail}&with=${partnerEmail}`);
        const messages = await res.json();
        renderChat(messages);
    } catch (err) { console.error(err); }
}

function renderChat(messages) {
    const box = document.getElementById('chatBox');
    let html = '';
    
    // Logic to find if the LAST message was a response to disable previous buttons
    messages.forEach((m, index) => {
        const isMine = m.senderEmail === myEmail;
        let content = '';

        if (m.type === 'IMAGE') {
            content = `<img src="${m.fileUrl}" class="img-fluid rounded" style="max-width:250px; cursor:pointer;" onclick="window.open(this.src)">`;
        } 
        else if (m.type === 'CALL_REQUEST') {
            // Check if there is a newer CALL_RESPONSE in history to hide buttons
            const hasResponse = messages.slice(index + 1).some(nextM => nextM.type === 'CALL_RESPONSE');
            
            content = `
                <div class="call-card shadow-sm text-dark">
                    <div class="fw-bold small mb-1"><i class="bi bi-camera-video me-1"></i> Meeting Request</div>
                    <p class="small mb-2">${m.message}</p>
                    ${(!isMine && !hasResponse) ? `
                        <div class="d-flex gap-2">
                            <button class="btn btn-sm btn-success flex-grow-1 fw-bold" onclick="startZegoCall('video')">Enable</button>
                            <button class="btn btn-sm btn-outline-danger flex-grow-1" onclick="respondToCall('Declined')">Disable</button>
                        </div>` : (hasResponse ? `<div class="call-status-msg text-muted"><i class="bi bi-slash-circle"></i> Request Handled</div>` : `<span class="badge bg-warning text-dark">Waiting...</span>`)}
                </div>`;
        } 
        else if (m.type === 'CALL_RESPONSE') {
            content = `<div class="call-status-msg fw-bold ${m.message.includes('Started') ? 'text-success' : 'text-danger'}">
                        <i class="bi ${m.message.includes('Started') ? 'bi-check-all' : 'bi-x-circle'}"></i> Call ${m.message}
                       </div>`;
        }
        else {
            content = `<span>${m.message}</span>`;
        }

        html += `
            <div class="message-wrapper ${isMine ? 'sent' : 'received'}">
                <div class="message-content shadow-sm">
                    ${content}
                    <div style="font-size: 10px; opacity: 0.7; margin-top: 5px; text-align: right;">
                        ${new Date(m.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                    </div>
                </div>
            </div>`;
    });
    box.innerHTML = html;
    box.scrollTop = box.scrollHeight;
}

function showCallOverlay(type) {
    const overlay = document.getElementById('callRequestOverlay');
    document.getElementById('overlayPartner').innerText = partnerEmail;
    overlay.style.display = 'flex';
    document.getElementById('confirmSendCall').onclick = () => { requestCall(type); hideCallOverlay(); };
}

function hideCallOverlay() { document.getElementById('callRequestOverlay').style.display = 'none'; }

async function requestCall(type) {
    await postMessage(`I would like to have a ${type} call.`, 'CALL_REQUEST');
}

async function respondToCall(status) {
    await postMessage(status, 'CALL_RESPONSE');
}

async function postMessage(text, type) {
    const payload = { senderEmail: myEmail, receiverEmail: partnerEmail, message: text, type: type };
    await fetch('https://hirbee-1.onrender.com/api/messages/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    loadMessages();
}

async function startZegoCall(callType) {
    await respondToCall('Started'); // Send response to hide buttons on both sides
    const appID = 2067388199; 
    const serverSecret = "5ea9bb5f908480c4de623bb70b6cd3e1";
    const roomID = [myEmail, partnerEmail].sort().join("_");

    const kitToken = ZegoUIKitPrebuilt.generateKitTokenForTest(appID, serverSecret, roomID, myEmail, myName);
    zpInstance = ZegoUIKitPrebuilt.create(kitToken);

    zpInstance.joinRoom({
        container: document.getElementById('callContainer'),
        scenario: { mode: ZegoUIKitPrebuilt.OneONoneCall },
        turnOnCameraWhenJoining: true,
        showPreJoinView: false,
        onLeaveRoom: () => handleEndCall()
    });

    new bootstrap.Modal(document.getElementById('callModal')).show();
}

async function handleEndCall() {
    await respondToCall('Ended'); // Send end message
    if (zpInstance) zpInstance.destroy();
    location.reload(); 
}

async function sendMessage() {
    const input = document.getElementById('msgInput');
    if (!input.value.trim() || !partnerEmail) return;
    await postMessage(input.value.trim(), 'TEXT');
    input.value = '';
}

async function handleFileUpload(input) {
    const file = input.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (e) => {
        const payload = { senderEmail: myEmail, receiverEmail: partnerEmail, message: "Sent an image", fileUrl: e.target.result, type: 'IMAGE' };
        await fetch('https://hirbee-1.onrender.com/api/messages/send', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
        });
        loadMessages();
    };
    reader.readAsDataURL(file);
}