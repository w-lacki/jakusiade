flatpickr("#searchDate", {
    enableTime: true,
    time_24hr: true,
    minuteIncrement: 1,
    altInput: true,
    altFormat: "d.m.Y H:i",
    dateFormat: "Y-m-d\\TH:i:00"
});

function debounce(func, delay) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), delay);
    };
}

function setupAutocomplete(nameInputId, idInputId, suggestionsId) {
    const nameInput = document.getElementById(nameInputId);
    const idInput = document.getElementById(idInputId);
    const suggestionsList = document.getElementById(suggestionsId);

    const fetchStations = async (query) => {
        if (query.length < 2) {
            suggestionsList.style.display = 'none';
            return;
        }
        try {
            const response = await fetch(`/search/station/${encodeURIComponent(query)}`);
            const stations = await response.json();

            suggestionsList.innerHTML = '';
            if (stations.length > 0) {
                stations.forEach(station => {
                    const li = document.createElement('li');
                    li.textContent = station.name;
                    li.onclick = () => {
                        nameInput.value = station.name;
                        idInput.value = station.id;
                        suggestionsList.style.display = 'none';
                    };
                    suggestionsList.appendChild(li);
                });
                suggestionsList.style.display = 'block';
            } else {
                suggestionsList.style.display = 'none';
            }
        } catch (error) {
            console.error("Error fetching stations:", error);
        }
    };

    nameInput.addEventListener('input', debounce((e) => fetchStations(e.target.value), 300));

    document.addEventListener('click', (e) => {
        if (e.target !== nameInput && e.target !== suggestionsList) {
            suggestionsList.style.display = 'none';
        }
    });
}

setupAutocomplete('startStationName', 'startStationId', 'startStationSuggestions');
setupAutocomplete('endStationName', 'endStationId', 'endStationSuggestions');

document.getElementById('searchBtn').addEventListener('click', async () => {
    const startId = document.getElementById('startStationId').value;
    const endId = document.getElementById('endStationId').value;
    let dateVal = document.getElementById('searchDate').value;
    console.log(dateVal)
    const resultsContainer = document.getElementById('resultsContainer');

    if (!startId || !endId || !dateVal) {
        alert("Proszę wybrać prawidłową stację początkową, docelową oraz datę.");
        return;
    }

    resultsContainer.innerHTML = '<em>Wyszukiwanie...</em>';

    try {
        const url = `/search/connection?start=${startId}&end=${endId}&date=${encodeURIComponent(dateVal)}`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(await response.text());
        }

        const connections = await response.json();
        renderResults(connections);
    } catch (error) {
        resultsContainer.innerHTML = `<p style="color: red;">Error: ${error.message}</p>`;
    }
});

function renderResults(connections) {
    const container = document.getElementById('resultsContainer');
    container.innerHTML = '';

    if (connections.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #6b7280;">Nie znaleziono połączeń w wybranym terminie.</p>';
        return;
    }

    connections.forEach(conn => {
        const depTime = new Date(conn.departure).toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit', hour12: false });
        const arrTime = new Date(conn.arrival).toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit', hour12: false });
        const dateStr = new Date(conn.departure).toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' });
        const connId = conn.uuid;

        let html = `
            <div class="result-card" id="card-${connId}">
                <div class="result-header">
                    <div class="time-block">
                        <span>${depTime}</span>
                        <span class="time-arrow">&rarr;</span>
                        <span>${arrTime}</span>
                    </div>
                    <div class="connection-meta">
                        ${conn.duration} min | Przesiadki: ${conn.changes}
                    </div>
                </div>
                
                <div style="font-size: 0.85rem; color: #6b7280; margin-bottom: 15px;">Data wyjazdu: ${dateStr}</div>
                
                <div class="leg-container">
        `;

        conn.legs.forEach(leg => {
            if (leg.leg_type === "train_leg") {
                const legDep = new Date(leg.departure).toLocaleTimeString('pl-PL', {hour: '2-digit', minute: '2-digit', hour12: false});
                const legArr = new Date(leg.arrival).toLocaleTimeString('pl-PL', {hour: '2-digit', minute: '2-digit', hour12: false});
                html += `
                    <div class="train-leg">
                        <strong>${leg.train_full_name || leg.train_name}</strong><br>
                        <span style="color: #6b7280;">${legDep} (Peron ${leg.departure_platform || '-'}) &mdash; ${legArr} (Peron ${leg.arrival_platform || '-'})</span>
                    </div>
                `;
            } else {
                html += `
                    <div class="train-leg transfer-leg">
                        Czas na przesiadkę: ${leg.duration} min
                    </div>
                `;
            }
        });

        html += `
                </div>
                <button class="btn-outline" onclick="checkSeats('${connId}')">Sprawdź wolne miejsca</button>
                <div id="seats-${connId}"></div>
            </div>
        `;
        container.innerHTML += html;
    });
}

async function checkSeats(connectionUuid) {
    const seatsContainer = document.getElementById(`seats-${connectionUuid}`);
    seatsContainer.innerHTML = '<div style="margin-top:15px; color: var(--text-muted);"><em>Sprawdzanie dostępności...</em></div>';

    try {
        const response = await fetch(`/search/seat/${connectionUuid}`);

        if (!response.ok) {
            throw new Error("Nie udało się pobrać danych o miejscach.");
        }

        const seatData = await response.json();

        if (!seatData.seats || Object.keys(seatData.seats).length === 0) {
            seatsContainer.innerHTML = `
                <div class="seat-error">
                    Brak miejsc podlegających rezerwacji dla tego połączenia.
                </div>`;
            return;
        }

        let html = `<div class="seats-container">`;

        seatData.seats.forEach(trainGroup => {
            const trainName = trainGroup.trainName;
            const seatsArray = trainGroup.seatsList;

            html += `
                <div class="train-group">
                    <div class="train-group-title">${trainName}</div>
            `;

            if (seatsArray.length === 0) {
                html += `<div style="font-size: 0.9em; color: var(--text-muted);">Brak wolnych miejsc na tym odcinku.</div>`;
            } else {
                seatsArray.forEach(seat => {
                    const durationMins = Math.floor(seat.duration / 60);
                    html += `
                        <div class="seat-item">
                            <span><strong>Wagon:</strong> ${seat.seatCarriage} | <strong>Miejsce:</strong> ${seat.seatNumber}</span>
                            <span style="color: var(--text-muted); font-size: 0.85em;">
                                ${seat.startStop} &rarr; ${seat.endStop} (${durationMins} min)
                            </span>
                        </div>
                    `;
                });
            }
            html += `</div>`;
        });

        html += `</div>`;
        seatsContainer.innerHTML = html;

    } catch (error) {
        seatsContainer.innerHTML = `<div class="seat-error">Błąd: ${error.message}</div>`;
    }
}