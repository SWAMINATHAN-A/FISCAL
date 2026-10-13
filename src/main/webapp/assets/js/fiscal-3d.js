/**
 * FISCAL — 3D Interactive DBMS Architecture Visualizer
 * Built with Three.js (r128)
 * Theme: Matcha (#65753B), Deep Forest Olive, Linen Porcelain & Architectural Wireframe
 * Features:
 *  - 3-Tier Isometric Relational Database Storage Cylinder Stack
 *  - Oracle Datafile & Buffer Cache Emitting Core
 *  - Orbiting Relational Entity Nodes (Taxpayers, Assessments, Ledger, Audit)
 *  - Animated Query Data Pulses & ForeignKey Relational Vectors
 *  - Dual Concentric Query Execution Rings
 *  - Smooth Inertial Mouse Parallax & Gyroscopic Float
 *  - Interactive Click / Hover Pulse Excitation & Realtime Telemetry HUD
 */

(function () {
  'use strict';

  function initDbms3D() {
    const container = document.getElementById('dbms-3d-wrapper');
    const canvas = document.getElementById('dbms-3d-canvas');
    if (!container || !canvas || typeof THREE === 'undefined') {
      return;
    }

    // 1. Scene & Camera Setup
    const scene = new THREE.Scene();

    const getDimensions = () => {
      const rect = container.getBoundingClientRect();
      const width = rect.width > 0 ? rect.width : 540;
      const height = rect.height > 0 ? rect.height : 380;
      return { width, height };
    };

    let { width, height } = getDimensions();

    const camera = new THREE.PerspectiveCamera(37, width / height, 0.1, 1000);
    camera.position.set(7.2, 5.4, 7.2);
    camera.lookAt(0, 0, 0);

    const renderer = new THREE.WebGLRenderer({
      canvas: canvas,
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance'
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.shadowMap.enabled = false;

    // 2. Lighting Rig
    const ambientLight = new THREE.AmbientLight(0xfaf7eb, 0.95);
    scene.add(ambientLight);

    const keyLight = new THREE.DirectionalLight(0xffffff, 1.25);
    keyLight.position.set(8, 12, 7);
    scene.add(keyLight);

    const rimLight = new THREE.DirectionalLight(0x8da054, 0.9);
    rimLight.position.set(-8, -4, -6);
    scene.add(rimLight);

    const coreLight = new THREE.PointLight(0xa5b868, 1.4, 8);
    coreLight.position.set(0, 0, 0);
    scene.add(coreLight);

    // 3. Color Palette Definitions (Matcha & Architectural Porcelain)
    const PALETTE = {
      matchaDeep: 0x3d4b22,
      matchaMid: 0x5a6a35,
      matchaLight: 0x7a8c48,
      matchaSoft: 0x9ca763,
      matchaGlow: 0xb5c67d,
      wireframe: 0xabb688,
      wireframeBright: 0xc8d4a6,
      creamAccent: 0xe8e6d9,
      nodeFill: 0x48582b
    };

    // Master Group for Mouse Rotation
    const masterGroup = new THREE.Group();
    scene.add(masterGroup);

    // 4. Central Database Cylinder Stack (3 Tiers)
    const cylinderGroup = new THREE.Group();
    masterGroup.add(cylinderGroup);

    const cylinderRadius = 2.1;
    const cylinderHeight = 0.52;
    const cylinderSegments = 48;
    const tierSpacing = 0.72;

    const cylinderMaterial = new THREE.MeshStandardMaterial({
      color: PALETTE.matchaMid,
      roughness: 0.38,
      metalness: 0.42,
      flatShading: false
    });

    const tierMaterials = [
      new THREE.MeshStandardMaterial({ color: PALETTE.matchaDeep, roughness: 0.45, metalness: 0.38 }), // Base (Datafiles)
      new THREE.MeshStandardMaterial({ color: PALETTE.matchaMid, roughness: 0.36, metalness: 0.46 }),  // Mid (Tablespaces)
      new THREE.MeshStandardMaterial({ color: PALETTE.matchaLight, roughness: 0.30, metalness: 0.50 }) // Top (SGA Cache)
    ];

    const cylinderTiers = [];

    for (let i = 0; i < 3; i++) {
      const yPos = (i - 1) * tierSpacing;
      const cylGeo = new THREE.CylinderGeometry(cylinderRadius, cylinderRadius, cylinderHeight, cylinderSegments);
      const cylMesh = new THREE.Mesh(cylGeo, tierMaterials[i]);
      cylMesh.position.y = yPos;

      // Subtle edge outlines for architectural blueprint styling
      const edgeGeo = new THREE.EdgesGeometry(cylGeo, 24);
      const edgeMat = new THREE.LineBasicMaterial({
        color: PALETTE.wireframeBright,
        transparent: true,
        opacity: 0.55
      });
      const edgeLine = new THREE.LineSegments(edgeGeo, edgeMat);
      cylMesh.add(edgeLine);

      // Add peripheral block notches around cylinder rim (representing database extent blocks)
      const notchCount = 12;
      const notchGeo = new THREE.BoxGeometry(0.12, 0.28, 0.08);
      const notchMat = new THREE.MeshStandardMaterial({
        color: PALETTE.creamAccent,
        roughness: 0.3,
        metalness: 0.5
      });

      for (let n = 0; n < notchCount; n++) {
        const angle = (n / notchCount) * Math.PI * 2;
        const notch = new THREE.Mesh(notchGeo, notchMat);
        notch.position.set(
          Math.cos(angle) * (cylinderRadius + 0.02),
          0,
          Math.sin(angle) * (cylinderRadius + 0.02)
        );
        notch.rotation.y = -angle;
        cylMesh.add(notch);
      }

      cylinderGroup.add(cylMesh);
      cylinderTiers.push({ mesh: cylMesh, initialY: yPos, phase: i * 0.8 });
    }

    // Glowing Central Spindle / Oracle Data Bus
    const spindleGeo = new THREE.CylinderGeometry(0.55, 0.55, 2.3, 24);
    const spindleMat = new THREE.MeshStandardMaterial({
      color: PALETTE.matchaGlow,
      emissive: 0x6e8436,
      emissiveIntensity: 0.4,
      roughness: 0.2,
      metalness: 0.7
    });
    const spindle = new THREE.Mesh(spindleGeo, spindleMat);
    cylinderGroup.add(spindle);

    // 5. Dual Orbital Query Execution Rings
    const ringGroup = new THREE.Group();
    masterGroup.add(ringGroup);

    // Inner Query Ring
    const innerRingGeo = new THREE.TorusGeometry(3.2, 0.02, 16, 72);
    const ringMat1 = new THREE.MeshBasicMaterial({
      color: PALETTE.matchaSoft,
      transparent: true,
      opacity: 0.65
    });
    const innerRing = new THREE.Mesh(innerRingGeo, ringMat1);
    innerRing.rotation.x = Math.PI / 2;
    ringGroup.add(innerRing);

    // Outer Query Ring (tilted slightly for dimensional depth)
    const outerRingGeo = new THREE.TorusGeometry(4.0, 0.016, 16, 80);
    const ringMat2 = new THREE.MeshBasicMaterial({
      color: PALETTE.wireframeBright,
      transparent: true,
      opacity: 0.45
    });
    const outerRing = new THREE.Mesh(outerRingGeo, ringMat2);
    outerRing.rotation.x = (Math.PI / 2) + 0.25;
    outerRing.rotation.y = 0.15;
    ringGroup.add(outerRing);

    // 6. Orbiting Relational Entity Nodes & ForeignKey Link Vectors
    const nodesData = [
      { name: 'TAXPAYERS', angle: 0.2, radius: 3.5, y: 0.6, speed: 0.003 },
      { name: 'ASSESSMENTS', angle: 1.8, radius: 3.8, y: -0.3, speed: 0.0025 },
      { name: 'PAYMENTS', angle: 3.4, radius: 3.3, y: 0.4, speed: 0.0035 },
      { name: 'AUDIT_LOGS', angle: 4.8, radius: 3.6, y: -0.5, speed: 0.0028 }
    ];

    const nodesGroup = new THREE.Group();
    masterGroup.add(nodesGroup);

    const nodeGeo = new THREE.BoxGeometry(0.5, 0.28, 0.5);
    const nodeMat = new THREE.MeshStandardMaterial({
      color: PALETTE.nodeFill,
      roughness: 0.35,
      metalness: 0.45
    });
    const nodeEdgeMat = new THREE.LineBasicMaterial({ color: PALETTE.matchaGlow, linewidth: 1.5 });

    const activeNodes = [];
    const linkLines = [];
    const packetPulses = [];

    nodesData.forEach((data, index) => {
      const nodeMesh = new THREE.Mesh(nodeGeo, nodeMat);
      const edge = new THREE.LineSegments(new THREE.EdgesGeometry(nodeGeo), nodeEdgeMat);
      nodeMesh.add(edge);

      // Micro LED indicator on node
      const ledGeo = new THREE.BoxGeometry(0.1, 0.06, 0.1);
      const ledMat = new THREE.MeshBasicMaterial({ color: 0xddff88 });
      const led = new THREE.Mesh(ledGeo, ledMat);
      led.position.set(0, 0.16, 0);
      nodeMesh.add(led);

      nodesGroup.add(nodeMesh);

      // Vector Line from node to cylinder core
      const lineGeo = new THREE.BufferGeometry().setFromPoints([
        new THREE.Vector3(0, data.y, 0),
        new THREE.Vector3(0, data.y, 0)
      ]);
      const lineMat = new THREE.LineDashedMaterial({
        color: PALETTE.matchaSoft,
        dashSize: 0.18,
        gapSize: 0.12,
        transparent: true,
        opacity: 0.45
      });
      const line = new THREE.Line(lineGeo, lineMat);
      line.computeLineDistances();
      masterGroup.add(line);

      // Data Packet Pulse traveling along the vector
      const pulseGeo = new THREE.SphereGeometry(0.08, 12, 12);
      const pulseMat = new THREE.MeshBasicMaterial({ color: PALETTE.matchaGlow });
      const pulse = new THREE.Mesh(pulseGeo, pulseMat);
      masterGroup.add(pulse);

      activeNodes.push({
        mesh: nodeMesh,
        data: data,
        currentAngle: data.angle,
        radius: data.radius,
        baseY: data.y,
        speed: data.speed,
        pulseOffset: index * 0.25
      });

      linkLines.push(line);
      packetPulses.push(pulse);
    });

    // 7. Ambient Database Geometry Particles & Floor Radar Base
    const radarBaseGeo = new THREE.RingGeometry(1.6, 4.3, 48, 1);
    const radarBaseMat = new THREE.MeshBasicMaterial({
      color: PALETTE.wireframe,
      transparent: true,
      opacity: 0.08,
      side: THREE.DoubleSide
    });
    const radarBase = new THREE.Mesh(radarBaseGeo, radarBaseMat);
    radarBase.rotation.x = Math.PI / 2;
    radarBase.position.y = -1.6;
    masterGroup.add(radarBase);

    // Floating data particles
    const particleCount = 40;
    const particleGeo = new THREE.BufferGeometry();
    const particlePositions = new Float32Array(particleCount * 3);

    for (let p = 0; p < particleCount; p++) {
      const pAngle = Math.random() * Math.PI * 2;
      const pRad = 2.2 + Math.random() * 2.8;
      const pY = (Math.random() - 0.5) * 3.5;
      particlePositions[p * 3] = Math.cos(pAngle) * pRad;
      particlePositions[p * 3 + 1] = pY;
      particlePositions[p * 3 + 2] = Math.sin(pAngle) * pRad;
    }
    particleGeo.setAttribute('position', new THREE.BufferAttribute(particlePositions, 3));

    const particleMat = new THREE.PointsMaterial({
      color: PALETTE.matchaSoft,
      size: 0.08,
      transparent: true,
      opacity: 0.6
    });
    const particles = new THREE.Points(particleGeo, particleMat);
    masterGroup.add(particles);

    // 8. Mouse Parallax & Dynamic Damping Logic
    let mouseX = 0;
    let mouseY = 0;
    let targetRotationX = 0;
    let targetRotationY = 0;
    let currentRotationX = 0;
    let currentRotationY = 0;
    let isHovered = false;
    let clickPulseIntensity = 0;

    const brandPanel = container.closest('.login-brand-panel') || container;

    function onMouseMove(event) {
      const rect = brandPanel.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;

      // Normalized coordinates (-1 to 1)
      const normX = (x / rect.width) * 2 - 1;
      const normY = -(y / rect.height) * 2 + 1;

      targetRotationY = normX * 0.45;
      targetRotationX = -normY * 0.35;
    }

    brandPanel.addEventListener('mousemove', onMouseMove);
    brandPanel.addEventListener('mouseenter', () => { isHovered = true; });
    brandPanel.addEventListener('mouseleave', () => {
      isHovered = false;
      targetRotationX = 0;
      targetRotationY = 0;
    });

    // Cycling SQL execution feedback simulation
    const sqlQueries = [
      'EXEC: SELECT * FROM TAXPAYERS (0.12ms)',
      'QUERY: B-TREE INDEX SCAN ON TAX_ID (0.08ms)',
      'TXN: INSERT INTO TAX_ASSESSMENT (0.24ms)',
      'ORCL_SGA: BUFFER HIT RATIO 99.8%',
      'COMMIT WORK: WAL LOG FLUSHED (0.16ms)'
    ];
    let queryIndex = 0;

    // Excitation pulse on click / tap
    function onTriggerPulse() {
      clickPulseIntensity = 1.0;
      const nextQuery = sqlQueries[queryIndex % sqlQueries.length];
      queryIndex++;
      triggerHudFeedback(nextQuery);
    }

    container.addEventListener('click', onTriggerPulse);

    // Touch support for mobile / tablets
    brandPanel.addEventListener('touchmove', (e) => {
      if (e.touches && e.touches[0]) {
        const touch = e.touches[0];
        const rect = brandPanel.getBoundingClientRect();
        const x = touch.clientX - rect.left;
        const y = touch.clientY - rect.top;
        const normX = (x / rect.width) * 2 - 1;
        const normY = -(y / rect.height) * 2 + 1;
        targetRotationY = normX * 0.45;
        targetRotationX = -normY * 0.35;
      }
    }, { passive: true });

    // Interactive HUD elements
    const hudBadge = document.getElementById('dbms-hud-text');
    let hudTimeout = null;

    function triggerHudFeedback(msg) {
      if (!hudBadge) return;
      hudBadge.textContent = msg;
      hudBadge.classList.add('pulse-active');
      if (hudTimeout) clearTimeout(hudTimeout);
      hudTimeout = setTimeout(() => {
        hudBadge.textContent = 'ORACLE 19c ENGINE • REPLICA SYNC';
        hudBadge.classList.remove('pulse-active');
      }, 3000);
    }

    // 9. Resize Handling & ResizeObserver
    function handleResize() {
      const dims = getDimensions();
      width = dims.width;
      height = dims.height;
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      renderer.setSize(width, height);
    }

    window.addEventListener('resize', handleResize);
    if (window.ResizeObserver) {
      const ro = new ResizeObserver(() => {
        handleResize();
      });
      ro.observe(container);
    }

    // 10. Main Animation Loop
    let clock = new THREE.Clock();

    function animate() {
      requestAnimationFrame(animate);

      const elapsedTime = clock.getElapsedTime();
      const delta = clock.getDelta();

      // Smooth inertia rotation toward mouse target
      currentRotationX += (targetRotationX - currentRotationX) * 0.05;
      currentRotationY += (targetRotationY - currentRotationY) * 0.05;

      // Base idle continuous rotation
      masterGroup.rotation.y = currentRotationY + (elapsedTime * 0.12);
      masterGroup.rotation.x = currentRotationX + (Math.sin(elapsedTime * 0.5) * 0.04);
      masterGroup.position.y = Math.sin(elapsedTime * 1.2) * 0.08;

      // Tiers breathing animation (vertical micro-oscillation)
      cylinderTiers.forEach((tier, i) => {
        const breathing = Math.sin(elapsedTime * 1.5 + tier.phase) * 0.035;
        tier.mesh.position.y = tier.initialY + breathing;
      });

      // Rings differential rotation
      innerRing.rotation.z = elapsedTime * 0.2;
      outerRing.rotation.z = -elapsedTime * 0.15;

      // Relational Nodes update & Packet Pulses
      activeNodes.forEach((node, i) => {
        node.currentAngle += node.speed;
        const nx = Math.cos(node.currentAngle) * node.radius;
        const nz = Math.sin(node.currentAngle) * node.radius;
        const ny = node.baseY + Math.sin(elapsedTime * 2 + i) * 0.12;

        node.mesh.position.set(nx, ny, nz);
        node.mesh.rotation.y = -node.currentAngle + Math.PI / 4;
        node.mesh.rotation.x = Math.sin(elapsedTime + i) * 0.1;

        // Update vector link lines
        const line = linkLines[i];
        const positions = line.geometry.attributes.position.array;
        positions[0] = 0; // Center core X
        positions[1] = ny * 0.4; // Center core Y
        positions[2] = 0; // Center core Z
        positions[3] = nx;
        positions[4] = ny;
        positions[5] = nz;
        line.geometry.attributes.position.needsUpdate = true;
        line.computeLineDistances();

        // Update packet pulse along connection
        const pulse = packetPulses[i];
        const pulseProgress = (Math.sin(elapsedTime * 2.2 + node.pulseOffset * 4) + 1) / 2;
        pulse.position.lerpVectors(new THREE.Vector3(0, ny * 0.4, 0), new THREE.Vector3(nx, ny, nz), pulseProgress);
        pulse.scale.setScalar(0.8 + Math.sin(elapsedTime * 6 + i) * 0.2);
      });

      // Handle excitation click pulse decay
      if (clickPulseIntensity > 0.01) {
        clickPulseIntensity *= 0.94;
        coreLight.intensity = 1.4 + clickPulseIntensity * 3.5;
        spindle.scale.set(
          1 + clickPulseIntensity * 0.15,
          1,
          1 + clickPulseIntensity * 0.15
        );
      } else {
        coreLight.intensity = 1.4 + Math.sin(elapsedTime * 3) * 0.3;
        spindle.scale.set(1, 1, 1);
      }

      // Rotate particle field slowly
      particles.rotation.y = -elapsedTime * 0.04;

      renderer.render(scene, camera);
    }

    animate();
  }

  // Initialize once DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initDbms3D);
  } else {
    initDbms3D();
  }
})();
