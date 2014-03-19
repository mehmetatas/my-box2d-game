package com.me.mygdxgame;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Array;

public class MyGdxGame implements ApplicationListener {
	private float scale;

	private final static float timeStep = 1 / 60f;
	private final static int velocityIterations = 8;
	private final static int positionIterations = 3;

	private final float worldHeight = 6;
	private final float worldWidth = 4;

	private float ballScale;

	private OrthographicCamera camera;
	private World world;
	private Box2DDebugRenderer renderer;

	private SpriteBatch spriteBatch;
	private Sprite ball1Sprite;
	private Sprite ball2Sprite;

	private Body ball1Body;
	private Body ball2Body;
	private Body ucgen;
	private final Vector2[] vertices = new Vector2[3];
	private MouseJoint mouseJoint;

	private final Vector3 testPoint = new Vector3();

	@Override
	public void create() {
		float w = Gdx.graphics.getWidth();

		scale = worldWidth / w;

		camera = new OrthographicCamera(worldWidth, worldHeight);
		camera.position.set(worldWidth / 2, worldHeight / 2, 0);
		camera.update();

		renderer = new Box2DDebugRenderer();
		spriteBatch = new SpriteBatch();

		world = new World(new Vector2(0, -9.8f), true);

		ballScale = ((0.15f) / scale) / 32;

		Texture texture = new Texture(Gdx.files.internal("12.png"));
		ball1Sprite = new Sprite(texture);
		ball1Sprite.setScale(ballScale);
		ball2Sprite = new Sprite(texture);
		ball2Sprite.setScale(ballScale);

		createWall(worldWidth / 2, 0.05f, worldWidth / 2, 0.05f);
		createWall(worldWidth / 2, 0.05f, worldWidth / 2, worldHeight - 0.05f);
		createWall(0.05f, worldHeight / 2, 0.05f, worldHeight / 2);
		createWall(0.05f, worldHeight / 2, worldWidth - 0.05f, worldHeight / 2);

		ball1Body = createBall(worldWidth / 2f - 1f, worldHeight * 0.8f, 0.15f, 2f, ball1Sprite);
		createUcgen();
		createL();

		final Vector2 mouseJointTarget = new Vector2();
		final MouseJointDef mouseJointDef = new MouseJointDef();

		Gdx.input.setInputProcessor(new InputAdapter() {
			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				camera.unproject(testPoint.set(screenX, screenY, 0));
				world.QueryAABB(new QueryCallback() {
					@Override
					public boolean reportFixture(Fixture fixture) {
						if (fixture.getBody() == ball1Body || fixture.getBody() == ball2Body) {
							mouseJointDef.bodyA = ucgen;
							mouseJointDef.bodyB = fixture.getBody();
							mouseJointDef.maxForce = 100000f;
							mouseJointDef.target.set(testPoint.x, testPoint.y);
							mouseJoint = (MouseJoint) world.createJoint(mouseJointDef);
							return true;
						}
						return false;
					}
				}, testPoint.x, testPoint.y, testPoint.x, testPoint.y);
				return true;
			}

			@Override
			public boolean touchUp(int screenX, int screenY, int pointer, int button) {
				camera.unproject(testPoint.set(screenX, screenY, 0));
				if (mouseJoint != null) {
					world.destroyJoint(mouseJoint);
					mouseJoint = null;
					return true;
				}
				return false;
			}

			@Override
			public boolean touchDragged(int screenX, int screenY, int pointer) {
				camera.unproject(testPoint.set(screenX, screenY, 0));
				if (mouseJoint != null) {
					mouseJoint.setTarget(mouseJointTarget.set(testPoint.x, testPoint.y));
					return true;
				}
				return false;
			}
		});
	}

	final Array<Body> bodies = new Array<Body>();

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		world.getBodies(bodies);

		world.step(timeStep, velocityIterations, positionIterations);
		renderer.render(world, camera.combined);
		spriteBatch.begin();
		for (Body body : bodies) {
			Sprite sprite = (Sprite) body.getUserData();
			if (sprite != null) {
				sprite.setPosition(
						(body.getPosition().x - ballScale * 64f * scale) / scale - 2f,
						(body.getPosition().y - ballScale * 64f * scale) / scale - 2f);
				sprite.setRotation(body.getAngle() * MathUtils.radDeg);
				sprite.draw(spriteBatch);
			}
		}
		spriteBatch.end();
	}

	private void createL() {
		// Uzun cubuk
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.position.set(new Vector2(worldWidth / 2f, ((float) Math.sqrt(3)) * 0.35f + 0.1f));

		Body body = world.createBody(bodyDef);

		PolygonShape shape = new PolygonShape();
		shape.setAsBox(1.5f, 0.05f);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = 0.5f;
		fixtureDef.friction = 0.2f;
		fixtureDef.restitution = 0.8f;
		fixtureDef.shape = shape;

		body.createFixture(fixtureDef);

		shape.dispose();

		// Kisa cubuk
		shape = new PolygonShape();
		shape.setAsBox(0.05f, 0.15f, new Vector2(1.45f, 0.2f), 0);

		fixtureDef.shape = shape;

		body.createFixture(fixtureDef);

		shape.dispose();

		// Kisa cubuk ustune top koy
		ball2Body = createBall(3.25f,
				(float) Math.sqrt(3) * 0.35f + 0.1f + 0.15f,
				0.15f,
				0.5f,
				ball2Sprite);

		// Uzun cubukla ucgeni bagla
		RevoluteJointDef jointDef = new RevoluteJointDef();
		jointDef.bodyA = body;
		jointDef.bodyB = ucgen;
		jointDef.localAnchorA.set(0, 0);
		jointDef.localAnchorB.set(0, ((float) Math.sqrt(3)) * 0.35f * 0.66f);

		world.createJoint(jointDef);
	}

	private void createUcgen() {
		vertices[0] = new Vector2(worldWidth / 2f - 0.35f, 0.1f);
		vertices[1] = new Vector2(worldWidth / 2f + 0.35f, 0.1f);
		vertices[2] = new Vector2(worldWidth / 2f, ((float) Math.sqrt(3)) * 0.35f + 0.1f);

		float cx = (vertices[0].x + vertices[1].x + vertices[2].x) / 3f;
		float cy = (vertices[0].y + vertices[1].y + vertices[2].y) / 3f;

		for (Vector2 v : vertices) {
			v.x -= cx;
			v.y -= cy;
		}

		BodyDef ucgenDef = new BodyDef();
		ucgenDef.type = BodyType.StaticBody;
		ucgenDef.position.set(new Vector2(cx, cy));

		ucgen = world.createBody(ucgenDef);

		PolygonShape ucgenShape = new PolygonShape();
		ucgenShape.set(vertices);

		FixtureDef ucgenFixtureDef = new FixtureDef();
		ucgenFixtureDef.density = 0.0f;
		ucgenFixtureDef.shape = ucgenShape;

		ucgen.createFixture(ucgenFixtureDef);

		ucgenShape.dispose();
	}

	private void createWall(float halfWidth, float halfHeight, float x, float y) {
		BodyDef groundDef = new BodyDef();
		groundDef.type = BodyType.StaticBody;
		groundDef.position.set(new Vector2(x, y));

		Body ground = world.createBody(groundDef);

		PolygonShape groundShape = new PolygonShape();
		groundShape.setAsBox(halfWidth, halfHeight);

		FixtureDef groundFixtureDef = new FixtureDef();
		groundFixtureDef.density = 0.0f;
		groundFixtureDef.shape = groundShape;

		ground.createFixture(groundFixtureDef);

		groundShape.dispose();
	}

	private Body createBall(float x, float y, float r, float density, Sprite sprite) {
		BodyDef ballDef = new BodyDef();
		ballDef.type = BodyType.DynamicBody;
		ballDef.position.set(x, y);

		Body ball = world.createBody(ballDef);
		ball.setUserData(sprite);

		CircleShape ballShape = new CircleShape();
		ballShape.setRadius(r);

		FixtureDef ballFixtureDef = new FixtureDef();
		ballFixtureDef.density = density;
		ballFixtureDef.friction = 0.2f;
		ballFixtureDef.restitution = 0.8f;
		ballFixtureDef.shape = ballShape;

		ball.createFixture(ballFixtureDef);

		ballShape.dispose();

		return ball;
	}

	@Override
	public void dispose() {

	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
