// Dynamic Leaflet map + live updates via REST + WebSocket

document.addEventListener("DOMContentLoaded", () => {
    console.log("Map.js loaded ✅");
    const mapEl = document.getElementById('map');
    if(!mapEl) return;

    // Base map centered roughly on Kigali - RESPONSIVE
    const isMobile = window.innerWidth < 768;
    const isTablet = window.innerWidth >= 768 && window.innerWidth < 1024;
    
    const map = L.map('map', { 
        zoomControl: !isMobile, // Hide default zoom on mobile
        touchZoom: true,
        scrollWheelZoom: !isMobile, // Disable scroll zoom on mobile to prevent accidental zooming
        dragging: true,
        tap: true,
        tapTolerance: 15 // Larger tap tolerance for touch devices
    }).setView([-1.95, 30.058], isMobile ? 11 : 12);
    
    const satelliteLayer = L.tileLayer('https://{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
        maxZoom: 20,
        subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
        attribution: '© Google Maps'
    });

    const normalLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '© OpenStreetMap contributors'
    });

    satelliteLayer.addTo(map);

    const baseMaps = {
        "Satellite Map": satelliteLayer,
        "Normal Map": normalLayer
    };

    L.control.layers(baseMaps).addTo(map);
    
    // Add custom zoom control for mobile (bottom-right)
    if (isMobile) {
        L.control.zoom({
            position: 'bottomright'
        }).addTo(map);
    }
    
    // Handle window resize to invalidate map size
    let resizeTimeout;
    window.addEventListener('resize', function() {
        clearTimeout(resizeTimeout);
        resizeTimeout = setTimeout(function() {
            map.invalidateSize();
        }, 250);
    });

    const clusterGroup = L.markerClusterGroup({
        disableClusteringAtZoom: isMobile ? 14 : 16, // Cluster longer on mobile for performance
        spiderfyOnEveryZoom: false,
        showCoverageOnHover: !isMobile, // Disable on touch devices
        maxClusterRadius: isMobile ? 80 : 60, // Larger clusters on mobile for easier tapping
        spiderfyDistanceMultiplier: isMobile ? 1.5 : 1, // More spacing on mobile
        iconCreateFunction: function(cluster){
            const count = cluster.getChildCount();
            let sev = 'low';
            cluster.getAllChildMarkers().forEach(m => {
                const s = m.options.severity || 'MEDIUM';
                if(s === 'CRITICAL') sev = 'critical';
                else if(s === 'HIGH' && sev !== 'critical') sev = 'high';
                else if(s === 'MEDIUM' && sev === 'low') sev = 'medium';
            });
            const size = isMobile ? 50 : 40; // Larger cluster icons on mobile
            return L.divIcon({
                html: `<div><span style="font-size: ${isMobile ? '14px' : '12px'}">${count}</span></div>`,
                className: 'marker-cluster marker-cluster-' + sev,
                iconSize: L.point(size, size)
            });
        }
    });
    map.addLayer(clusterGroup);

    const markerIndex = new Map(); // id -> marker
    let reportsCache = [];
    let regionsCache = [];
    let liveEnabled = true;

    // Filters will be applied from case-map.html controls
    function getFilters(){
        return {
            type: val('fltType'),
            status: val('fltStatus'),
            severity: val('fltSeverity'),
            region: val('fltRegion'),
            text: val('fltText').toLowerCase(),
            from: dateVal('fltFrom'),
            to: dateVal('fltTo')
        };
    }

    function statusColor(status){
        const s = String(status||'').toUpperCase();
        return ({
            PENDING:'#ffc107', IN_PROGRESS:'#fd7e14', RESOLVED:'#0d6efd'
        })[s] || 'purple';
    }

    function createMarker(r){
        if(r.latitude == null || r.longitude == null) return null;
        const statusC = statusColor(r.investigationStatus);
        const severity = r.severity || 'MEDIUM';
        const severityC = severityColor(severity);
        const size = severitySize(severity);
        const touchSize = isMobile ? size + 6 : size; // Larger touch targets on mobile
        const icon = L.divIcon({
            html:`<div style="width:${size}px;height:${size}px;background:${statusC};border:3px solid ${severityC};border-radius:50%;box-shadow:0 2px 8px rgba(0,0,0,0.3)"></div>`,
            className:'', iconSize:[touchSize,touchSize], iconAnchor:[touchSize/2,touchSize/2]
        });
        const m = L.marker([r.latitude, r.longitude], { icon, severity });
        const popupWidth = isMobile ? Math.min(window.innerWidth - 40, 300) : 320;
        m.bindPopup(popupHtml(r), { 
            maxWidth: popupWidth,
            closeButton: true,
            autoPan: true,
            autoPanPadding: [10, 10]
        });
        return m;
    }

    function severityColor(sev){
        const c = {LOW:'#28a745', MEDIUM:'#ffc107', HIGH:'#fd7e14', CRITICAL:'#dc3545'};
        return c[sev] || c.MEDIUM;
    }

    function severitySize(sev){
        const s = {LOW:14, MEDIUM:18, HIGH:22, CRITICAL:26};
        return s[sev] || s.MEDIUM;
    }

    function popupHtml(r){
        const regionName = r.region && r.region.name ? r.region.name : 'Unknown';
        const severity = r.severity || 'MEDIUM';
        const severityBadge = `<span class="badge" style="background:${severityColor(severity)}">${severity}</span>`;
        const statusBadge = `<span class="badge" style="background:${statusColor(r.investigationStatus)}">${r.investigationStatus}</span>`;
        const desc = r.description ? (r.description.length > 100 ? r.description.substring(0,100) + '...' : r.description) : '';
        return `<div class="case-popup">
            <div class="fw-bold mb-2 d-flex justify-content-between align-items-start">
                <span>${escapeHtml(r.pollutionCategory||'Unknown')}</span>
                <div class="d-flex gap-1">${severityBadge}${statusBadge}</div>
            </div>
            <div class="small mb-2">
                <div><strong>ID:</strong> ${r.id}</div>
                <div><strong>Region:</strong> ${escapeHtml(regionName)}</div>
                <div><strong>Location:</strong> ${escapeHtml(r.location||'Not specified')}</div>
                <div><strong>Reported:</strong> ${formatDateTime(r.reportedAt)}</div>
            </div>
            ${desc ? `<div class="small text-muted mb-2">${escapeHtml(desc)}</div>` : ''}
            ${r.mediaPath ? mediaFragment(r.mediaPath) : ''}
            <div class="mt-2"><button class="btn btn-sm btn-danger" onclick="focusCase(${r.id})"><i class="bi bi-crosshair"></i> Center</button></div>
        </div>`;
    }

    window.focusCase = function(id){
        const r = reportsCache.find(x => x.id === id);
        if(!r || r.latitude==null || r.longitude==null) return;
        map.setView([r.latitude, r.longitude], 16, { animate: true, duration: 0.5 });
        const marker = markerIndex.get(id);
        if(marker) setTimeout(() => marker.openPopup(), 600);
    };

    function mediaFragment(path){
        const lower = path.toLowerCase();
        if(lower.endsWith('.mp4')) return `<video src="${path}" style="max-width:100%;max-height:120px;border-radius:4px" controls></video>`;
        return `<img src="${path}" style="max-width:100%;max-height:120px;border-radius:4px;object-fit:cover" class="border mt-1" />`;
    }

    function renderMarkers(list){
        console.log(`🎯 renderMarkers called with ${list.length} reports`);
        clusterGroup.clearLayers();
        markerIndex.clear();
        let validMarkers = 0;
        let invalidMarkers = 0;
        list.forEach(r => {
            if (!r.latitude || !r.longitude) {
                console.warn(`⚠️ Report ${r.id} missing coordinates:`, {lat: r.latitude, lng: r.longitude, location: r.location});
                invalidMarkers++;
                return;
            }
            const m = createMarker(r);
            if(m){
                clusterGroup.addLayer(m);
                markerIndex.set(r.id, m);
                validMarkers++;
            } else {
                console.warn(`⚠️ createMarker returned null for report ${r.id}`);
                invalidMarkers++;
            }
        });
        console.log(`✅ Rendered ${validMarkers} markers, ${invalidMarkers} failed/invalid`);
        
        // Show warning if reports exist but none have coordinates
        if(list.length > 0 && validMarkers === 0){
            console.warn('⚠️ No reports have GPS coordinates! Reports must include latitude/longitude to appear on map.');
        }
    }

    function renderTable(list){
        const tbody = document.getElementById('casesBody');
        if(!tbody) return;
        if(list.length===0){
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-3">No cases match filters.</td></tr>';
            return;
        }
        tbody.innerHTML = list.sort((a,b)=> new Date(b.reportedAt)-new Date(a.reportedAt)).slice(0,100).map(r => {
            const regionName = r.region && r.region.name ? r.region.name : '';
            const severity = r.severity || 'MEDIUM';
            return `<tr>
                <td>${r.id}</td>
                <td>${escapeHtml(r.pollutionCategory||'')}</td>
                <td><span class="badge" style="background:${severityColor(severity)}">${severity}</span></td>
                <td>${escapeHtml(regionName)}</td>
                <td><span class="badge" style="background:${statusColor(r.investigationStatus)}">${r.investigationStatus}</span></td>
                <td>${r.assignedAuthority ? escapeHtml(r.assignedAuthority.fullName || r.assignedAuthority.username) : ''}</td>
                <td>${formatDateTime(r.reportedAt)}</td>
                <td><button class="btn btn-sm btn-outline-primary" onclick="focusCase(${r.id})" title="Center on map"><i class="bi bi-crosshair"></i></button></td>
            </tr>`;
        }).join('');
    }

    function applyMapFilters(){
        const f = getFilters();
        const filtered = reportsCache.filter(r => {
            if(f.type && (r.pollutionCategory||'').toUpperCase() !== f.type.toUpperCase()) return false;
            if(f.status && r.investigationStatus !== f.status) return false;
            if(f.severity && (r.severity||'MEDIUM') !== f.severity) return false;
            const regName = r.region && r.region.name ? r.region.name : '';
            if(f.region && regName !== f.region) return false;
            if(f.text){
                const blob = `${r.description||''} ${r.location||''}`.toLowerCase();
                if(!blob.includes(f.text)) return false;
            }
            if(f.from && (!r.reportedAt || new Date(r.reportedAt) < f.from)) return false;
            if(f.to && (!r.reportedAt || new Date(r.reportedAt) > endOfDay(f.to))) return false;
            return true;
        });
        renderMarkers(filtered);
        renderTable(filtered);
        updateFilterSummary(f, filtered.length);
        updateCaseCount(filtered.length);
    }
    window.applyMapFilters = applyMapFilters;

    function updateFilterSummary(filters, count){
        const summary = document.getElementById('filterSummary');
        if(!summary) return;
        const active = [];
        if(filters.type) active.push(`Type: ${filters.type}`);
        if(filters.investigationStatus) active.push(`Status: ${filters.investigationStatus}`);
        if(filters.severity) active.push(`Severity: ${filters.severity}`);
        if(filters.region) active.push(`Region: ${filters.region}`);
        if(filters.text) active.push(`Search: "${filters.text}"`);
        if(filters.from || filters.to) active.push('Date range');
        if(active.length === 0){
            summary.textContent = `Showing all ${count} cases`;
            summary.className = 'small text-muted';
        } else {
            summary.textContent = `${count} cases (${active.join(', ')})`;
            summary.className = 'small text-primary fw-semibold';
        }
    }

    function updateCaseCount(count){
        const el = document.getElementById('caseCount');
        if(el) el.textContent = `${count} case${count !== 1 ? 's' : ''}`;
    }

    function resetMapFilters(){ ['fltType','fltStatus','fltSeverity','fltRegion','fltFrom','fltTo','fltText'].forEach(id => setVal(id,'')); applyMapFilters(); }
    window.resetMapFilters = resetMapFilters;

    function refreshMap(){ loadReports().then(() => applyMapFilters()); }
    window.refreshMap = refreshMap;

    function fitAllMarkers(){
        const layerCount = clusterGroup.getLayers().length;
        console.log(`🗺️ Fitting ${layerCount} markers on map`);
        if(layerCount === 0) {
            console.log('⚠️ No markers to fit, centering on Kigali');
            map.setView([-1.95, 30.058], 12);
            return;
        }
        try {
            const bounds = clusterGroup.getBounds();
            map.fitBounds(bounds.pad(0.15));
            console.log('✅ Map bounds fitted to markers');
        } catch(e) {
            console.error('Failed to fit bounds:', e);
            map.setView([-1.95, 30.058], 12);
        }
    }
    window.fitAllMarkers = fitAllMarkers;

    function toggleLive(){
        liveEnabled = !liveEnabled;
        const btn = document.getElementById('liveBtn');
        if(btn){ btn.innerHTML = `<i class="bi bi-broadcast"></i> Live: ${liveEnabled?'ON':'OFF'}`; }
    }
    window.toggleLive = toggleLive;

    async function loadRegions(){
        try { const r = await fetch('/api/regions'); if(!r.ok) return; regionsCache = await r.json(); populateRegionFilter(); } catch {}
    }
    function populateRegionFilter(){
        const sel = document.getElementById('fltRegion');
        if(!sel) return; sel.innerHTML = '<option value="">All</option>' + regionsCache.map(x=>`<option value="${escapeAttr(x.name)}">${escapeHtml(x.name)}</option>`).join('');
    }

    async function loadReports(){
        try {
            console.log('📡 Fetching reports from API...');
            const res = await fetch('/api/reports/public');
            if(!res.ok) throw new Error('Failed to fetch reports: ' + res.investigationStatus);
            const all = await res.json();
            console.log(`📊 Received ${all.length} total reports from API`);
            
            // Show ALL reports on the map (including PENDING for visibility)
            // Authorities can see everything, citizens see their own
            reportsCache = all;
            console.log(`✅ Loaded ${reportsCache.length} reports into cache`);
            
            // Log sample data for debugging
            if(reportsCache.length > 0){
                const sample = reportsCache[0];
                console.log('Sample report:', {
                    id: sample.id,
                    type: sample.pollutionCategory,
                    status: sample.investigationStatus,
                    lat: sample.latitude,
                    lng: sample.longitude,
                    hasCoords: !!(sample.latitude && sample.longitude)
                });
            }
            
            applyMapFilters();
        } catch(e){ 
            console.error('❌ Failed to load reports:', e); 
            showError('Failed to load case data. Please refresh the page.');
        }
    }

    function showError(msg){
        const tbody = document.getElementById('casesBody');
        if(tbody) tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">${msg}</td></tr>`;
    }

    // Initial load with better error handling
    console.log('🚀 Initializing map data...');
    Promise.all([loadRegions(), loadReports()])
        .then(() => {
            console.log('✅ Map data loaded successfully');
            fitAllMarkers();
        })
        .catch(err => {
            console.error('❌ Failed to initialize map:', err);
            showError('Failed to initialize map. Please refresh the page.');
        });

    // Live updates if enabled with reconnection
    let wsReconnectAttempts = 0;
    const maxReconnect = 10;
    function updateWSStatus(status){
        const el = document.getElementById('wsStatus');
        if(!el) return;
        const cfg = {
            connecting: {color:'#ffc107',title:'Connecting...',show:true},
            connected: {color:'#28a745',title:'Live updates active',show:true},
            error: {color:'#dc3545',title:'Connection error',show:true},
            reconnecting: {color:'#fd7e14',title:'Reconnecting...',show:true}
        };
        const c = cfg[status] || cfg.connecting;
        el.style.color = c.color;
        el.title = c.title;
        el.style.display = c.show ? 'inline' : 'none';
    }
    function connectWS(){
        if(!window.SockJS || !window.Stomp) return;
        try {
            const socket = new SockJS('/ws');
            const client = Stomp.over(socket);
            client.debug = () => {};
            updateWSStatus('connecting');
            client.connect({}, () => {
                console.log('✅ WebSocket connected');
                wsReconnectAttempts = 0;
                updateWSStatus('connected');
                client.subscribe('/topic/reports', (message) => {
                    if(!liveEnabled) return;
                    try {
                        const rpt = JSON.parse(message.body);
                        if(rpt.investigationStatus === 'PENDING'){
                            const idx = reportsCache.findIndex(x => x.id === rpt.id);
                            if(idx >= 0) reportsCache.splice(idx, 1);
                        } else {
                            const idx = reportsCache.findIndex(x => x.id === rpt.id);
                            if(idx >= 0) reportsCache[idx] = rpt; else reportsCache.push(rpt);
                            showLiveNotif(rpt);
                        }
                        applyMapFilters();
                    } catch(err){ console.warn('Invalid live report', err); }
                });
            }, (err) => {
                console.error('WebSocket error', err);
                updateWSStatus('error');
                if(wsReconnectAttempts++ < maxReconnect){
                    setTimeout(connectWS, 3000 * Math.pow(1.5, wsReconnectAttempts - 1));
                    updateWSStatus('reconnecting');
                }
            });
        } catch(e){ console.error('WS setup failed', e); }
    }
    function showLiveNotif(r){
        const n = document.createElement('div');
        n.className = 'alert alert-info alert-dismissible fade show position-fixed';
        n.style.cssText = 'top:80px;right:20px;z-index:9999;min-width:300px;';
        n.innerHTML = `<strong>Live Update</strong><br>${escapeHtml(r.pollutionCategory||'Case')} - ${r.investigationStatus}<button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
        document.body.appendChild(n);
        setTimeout(() => n.remove(), 5000);
    }
    connectWS();

    // Utilities
    function val(id){ const el = document.getElementById(id); return el ? el.value : ''; }
    function setVal(id,v){ const el = document.getElementById(id); if(el) el.value = v; }
    function dateVal(id){ const v = val(id); return v ? new Date(v + 'T00:00:00') : null; }
    function endOfDay(d){ const dt = new Date(d); dt.setHours(23,59,59,999); return dt; }
    function formatDateTime(dt){ if(!dt) return ''; const d = new Date(dt); return d.toLocaleString(); }
    function escapeHtml(t){ const div = document.createElement('div'); div.textContent = t; return div.innerHTML; }
    function escapeAttr(t){ return (t||'').replace(/"/g,'&quot;'); }
});
